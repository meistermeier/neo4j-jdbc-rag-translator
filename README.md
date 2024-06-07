# Rag To Cypher Translator

> [!IMPORTANT]
> Archived because text2cypher is now part of the neo4j-jdbc driver (https://github.com/neo4j/neo4j-jdbc/pull/647)

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

You need to provide your OpenAI API access token as `OPEN_AI_TOKEN` environment variable or directly as a driver property.

There are some parameters that need to be provided and some that can be overwritten by the user as a driver property.

| parameter                 | type   | default                              |
|---------------------------|--------|--------------------------------------|
| indexName (**mandatory**) | String | _null_                               |
| embeddingModel            | String | text-embedding-ada-002               |
| chatModel                 | String | gpt-3.5-turbo                        |
| chatTemperature           | Double | 0.0                                  |
| openAIToken               | String | _environment variable OPEN_AI_TOKEN_ |

## Usage (not needed for running the tests)
The `RagToCypherTranslatorFactory` will register itself via the META-INF/service mechanism with the Neo4j JDBC Driver.
The required Open AI Token can be passed as a system environment variable or via properties passed to the JDBC driver itself. 

The Neo4j JDCB Driver can be configured in such a way that every statement to be executed goes automatically through the translation layer.
This is done by setting `enableSQLTranslation` to `true`, either as URL parameter or as driver property.

After this is done, every JDBC `Statement` call, e.g. `Statement#executeQuery`, will invoke the translator before executing the statement.
The translator will only kick in if they are prefixed with `🤖, `.
If a statement is prefixed accordingly, it will be interpreted as textual input for the machine, otherwise it will be treated as normal Cypher and executed as is.
The Open AI response is hopefully a working Cypher statement, as it will be executed.

Another option is to just put the translator on the classpath, configure the Open AI Token, but set `enableSQLTranslation` to `false` (which is the default).
JDBC provides `Connection#nativeSQL`, which you can call as needed then, i.e. like this `con.nativeSQL("🤖, how many IWasHere nodes are in the graph");` which should give you than a Cypher statement, that you can either use or amend as needed.

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
