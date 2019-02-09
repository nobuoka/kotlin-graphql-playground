package info.vividcode.playground.graphql

import graphql.GraphQL
import graphql.schema.idl.SchemaGenerator
import graphql.schema.StaticDataFetcher
import graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import graphql.schema.idl.SchemaParser

/**
 * See : [GraphQL Java - Getting started](https://www.graphql-java.com/documentation/master/getting-started/)
 */
fun main() {
    val schema = "type Query{hello: String}"
    val query = "{hello}"
    val runtimeWiring = newRuntimeWiring()
        .type("Query") { it.dataFetcher("hello", StaticDataFetcher("world")) }
        .build()

    val typeDefinitionRegistry = SchemaParser().parse(schema)
    val graphqlSchema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
    val graphql = GraphQL.newGraphQL(graphqlSchema).build()
    val executionResult = graphql.execute(query)

    println(executionResult.getData<Any>().toString())
    // Prints: {hello=world}
}
