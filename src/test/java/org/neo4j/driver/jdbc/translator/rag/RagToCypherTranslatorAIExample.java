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

import org.neo4j.driver.jdbc.Neo4jDriver;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;

public final class RagToCypherTranslatorAIExample {

	private RagToCypherTranslatorAIExample() {
	}

	public static void main(String... args) throws Exception {

		var openAIToken = System.getenv("OPEN_AI_TOKEN");
		if (openAIToken == null || openAIToken.isBlank()) {
			throw new IllegalArgumentException(
					"Please set a system environment variable named `OPEN_AI_TOKEN` containing your OpenAI token");
		}

		try (var con = Neo4jDriver.withSQLTranslation().fromEnv().orElseThrow()) {
			//  No cheating involved
			try (Statement statement = con.createStatement()) {
				statement.execute("/*+ NEO4J FORCE_CYPHER */MATCH (m:Message) detach delete m");
			}

			Statement stmnt = con.createStatement();
			stmnt.execute("Create a Message node with a property 'content' containing 'the dynamic duo rocks'");
			stmnt.close();

			stmnt = con.createStatement();
			ResultSet result = stmnt.executeQuery("How many nodes labeled 'Message' are in the graph.");
			while (result.next()) {
				System.out.println(result.getInt(1));
			}

			stmnt.close();

			try (Statement statement = con.createStatement()) {
				ResultSet resultSet = statement.executeQuery("/*+ NEO4J FORCE_CYPHER */MATCH (n:Message) return n.content as content limit 1");
				resultSet.next();
				String content = resultSet.getString("content");
				System.out.println(content);
			}

		}
	}

}
