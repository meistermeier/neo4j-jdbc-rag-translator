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
package org.neo4j.jdbc.translator.rag;

import org.junit.jupiter.api.Test;
import org.neo4j.jdbc.Neo4jDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class RagToCypherTranslatorAIExampleTest {

	private static final Logger LOG = LoggerFactory.getLogger("RagToCypherExample");

	@Test
	void letsTalkTextToCypher() throws Exception {

		var openAIToken = System.getenv("OPEN_AI_TOKEN");
		if (openAIToken == null || openAIToken.isBlank()) {
			throw new IllegalArgumentException(
				"Please set a system environment variable named `OPEN_AI_TOKEN` containing your OpenAI token");
		}

		try (var con = Neo4jDriver.withSQLTranslation()
			.withProperties(Map.of("indexName", "spring-ai-document-index", "openAIToken", openAIToken))
			.fromEnv().orElseThrow()) {
			try (var statement = con.createStatement()) {
				//  No cheating involved
				statement.execute("MATCH (m:Message) detach delete m");
				statement.execute("🤖, create a Message node with a property 'content' containing 'the dynamic duo rocks'");

				try (var result = statement.executeQuery("🤖, how many nodes labeled 'Message' are in the graph.")) {
					while (result.next()) {
						LOG.info("Result of count Message query: {}", result.getInt(1));
					}
				}

				String cypherStatement = "MATCH (n:Message) RETURN n.content AS content LIMIT 1";
				LOG.info("Executing raw cypher: {}", cypherStatement);
				try (var resultSet = statement.executeQuery(cypherStatement)) {
					resultSet.next();
					var content = resultSet.getString("content");
					LOG.info("Content of node: {}", content);
				}
			}
		}
	}
}
