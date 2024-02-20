/*
 * Copyright (c) 2023-2024 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.jdbc.translator.rag;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslator;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gerrit Meier
 */
public class RagToCypher implements SqlTranslator {

	private static final String DEFAULT_CHAT_MODEL = "gpt-3.5-turbo";
	private static final double DEFAULT_TEMPERATURE = 0.0;
	private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-ada-002";

	private static final Logger LOGGER = Logger.getLogger(RagToCypher.class.getName());

	private static final String SYSTEM_PROMPT_TEMPLATE = """
    You are an assistant that gives out Cypher code snippets.
    Use the information from the DOCUMENTS section only to provide accurate answers.
    Return just the code snippet without formatting. No descriptive text.
    Don't use any learned knowledge that is not within the DOCUMENTS section.
	
    DOCUMENTS:
    {documents}""";

	private static final String SEARCH_QUERY = """
   CALL db.index.vector.queryNodes($0, 10, $1)
   				YIELD node AS node, score
   				RETURN node.content AS content
      ORDER BY score DESC""";

	static final String PREFIX = "🤖, ";
	static final int PREFIX_LENGTH = PREFIX.length();

	private final Map<String, Object> config;
	private final OpenAiService openAiService;

	public RagToCypher(Map<String, Object> config) {
		this.openAiService = new OpenAiService(System.getenv("OPEN_AI_TOKEN"));
		this.config = config;
	}

	@Override
	public String translate(String searchString, DatabaseMetaData databaseMetaData) {

		if(!searchString.startsWith(PREFIX)) {
			return searchString;
		}

		searchString = searchString.substring(PREFIX_LENGTH, PREFIX_LENGTH + 1).toUpperCase(Locale.ROOT) + searchString.substring(PREFIX_LENGTH + 1);

		if (databaseMetaData == null) {
			throw new IllegalStateException("Database connection must be open");
		}

		try {
			// not putting this into the try-with-resource-block is intentional
			Connection connection = databaseMetaData.getConnection();

			PreparedStatement statement = connection.prepareStatement(SEARCH_QUERY);
			statement.setObject(1, this.config.get("indexName"));
			statement.setObject(2, this.createEmbedding(searchString));
			ResultSet resultSet = statement.executeQuery();
			StringBuilder sb = new StringBuilder();

			while (resultSet.next()) {
				sb.append(resultSet.getString("content")).append(System.lineSeparator());
			}
			statement.close();

			// OpenAi chat request with content
			String systemPrompt = SYSTEM_PROMPT_TEMPLATE.replace("{documents}", sb.toString());
			String result = result(searchString, systemPrompt);
			LOGGER.log(Level.INFO, result);
			return result;

		} catch (SQLException e) {
			throw new RuntimeException("Something went wrong when trying to convert the question to a Cypher statement.", e);
		}
	}

	private List<Double> createEmbedding(String searchTerm) {
		EmbeddingRequest request = EmbeddingRequest.builder().model((String) this.config.getOrDefault("embeddingModel", DEFAULT_EMBEDDING_MODEL)).input(List.of(searchTerm)).build();
		return this.openAiService.createEmbeddings(request).getData().stream().flatMap(embedding -> embedding.getEmbedding().stream()).toList();
	}

	private String result(String searchTerm, String promptTemplate) {
		ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), promptTemplate);
		ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), searchTerm);
		ChatCompletionRequest request = ChatCompletionRequest.builder()
				.messages(List.of(systemMessage, userMessage))
				.model((String) this.config.getOrDefault("chatModel", DEFAULT_CHAT_MODEL))
				.temperature((Double) this.config.getOrDefault("chatTemperature", DEFAULT_TEMPERATURE))
				.build();

		return this.openAiService.createChatCompletion(request).getChoices().get(0).getMessage().getContent();
	}
}
