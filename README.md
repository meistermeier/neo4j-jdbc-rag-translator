# Rag To Cypher Translator

A [Neo4j-JDBC driver](https://github.com/neo4j/neo4j-jdbc) SqlTranslator implementation that uses OpenAI to create Cypher statements from natural language description.

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