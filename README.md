# Rag To Cypher Translator

A [Neo4j-JDBC driver](https://github.com/neo4j/neo4j-jdbc) SqlTranslator implementation that uses OpenAI to create Cypher statements from natural language description.

## Preparation

The translator depends on an existing vector index with populated embeddings for information about the Cypher language.

For this PoC, I took the full reference PDF version of the [Cypher documentation](https://neo4j.com/docs/resources/docs-archive/#_cypher_query_language) and ran it through a PDF parser.
This was heavily inspired by the work done in the [Spring AI project](https://github.com/spring-projects/spring-ai), especially in the [`PagePdfDocumentReader`](https://github.com/spring-projects/spring-ai/blob/eecbef30fa2083e747de22424b29f77782051a30/document-readers/pdf-reader/src/main/java/org/springframework/ai/reader/pdf/PagePdfDocumentReader.java).

Because the current version uses the already created JDBC connection, this data needs to be present in the database.

## How it works

The translator will pass through all statements not starting with the prefix `🤖, ` (hey, this is just a PoC, let me have some fun) to avoid interpreting all valid SQL or Cypher as a prompt.

Within the translator, the first query that will be issued is the _similarity search_ with the provided search term on the configured index.

```cypher
CALL db.index.vector.queryNodes($0, 10, $1)
     YIELD node AS node, score
     RETURN node.content AS content
     ORDER BY score DESC
```

This will return the 10 best matching documents (pages) from the database.
Their content will be used then in a predefined system prompt as the only source of information.

The system prompt will get put together with the search term as a user prompt to an OpenAI chat request.
The response gets returned as (non verified) Cypher.

## Configuration

There are some parameters that need to be provided and some that can be overwritten by the user:

| parameter                 | type   | default                |
|---------------------------|--------|------------------------|
| indexName (**mandatory**) | String | _null_                 |
| embeddingModel            | String | text-embedding-ada-002 |
| chatModel                 | String | gpt-3.5-turbo          |
| chatTemperature           | Double | 0.0                    |

## Usage
The `RagToCypherTranslatorFactory` needs to be registered as a `org.neo4j.driver.jdbc.translator.spi.SqlTranslatorFactory` in the META-INF/service.

After this is done, every JDBC `Statement` call, e.g. `Statement#executeQuery`, will invoke the translator before executing the statement.
As a consequence, this means that every Cypher call needs to be marked with `/*+ NEO4J FORCE_CYPHER */`.

See the test `RegToCypherTranslatorAIExample` in the src/test folder for a working example.

## Examples

Node creation example:
```
Input:
Create a Message node with a property 'content' containing 'the dynamic duo rocks'
Output:
CREATE (:Message {content: 'the dynamic duo rocks'})
```

Count query generator:
```
Input:
How many nodes labeled 'Message' are in the graph.
Output:
MATCH (n:Message) RETURN count(n)
```