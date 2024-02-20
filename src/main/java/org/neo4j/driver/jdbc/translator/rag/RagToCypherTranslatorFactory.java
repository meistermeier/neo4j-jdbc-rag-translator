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

import org.neo4j.driver.jdbc.translator.spi.SqlTranslator;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslatorFactory;

import java.util.Map;

/**
 * @author Gerrit Meier
 */
public class RagToCypherTranslatorFactory implements SqlTranslatorFactory {

	@Override
	public SqlTranslator create(Map<String, Object> properties) {
		// should be in the properties that create the driver, but I haven't figured out how to apply this if using `.fromEnv()`.
		return new RagToCypher(properties);
	}

	@Override
	public String getName() {
		return "RAG to Cypher Translator";
	}
}
