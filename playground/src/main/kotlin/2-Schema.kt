package info.vividcode.playground.graphql

import graphql.GraphQL
import graphql.schema.*
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.RuntimeWiring

/**
 * See : [GraphQL Java - Schema](https://www.graphql-java.com/documentation/master/schema/)
 */
fun main() {
    val query = """
        {
          characters(idIn: ["1", "2"]) {
            id
            name
          }
        }
    """.trimIndent()

    val typeDefinitionRegistry = SchemaParser().parse(schema)
    val graphqlSchema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
    val graphql = GraphQL.newGraphQL(graphqlSchema).build()
    val executionResult = graphql.execute(query)

    println(executionResult)
    println(executionResult.getData<Any>())
    // Prints: {characters=[{id=1, name=碇 シンジ}, {id=2, name=碇 ユイ}]}
}

val schema = """
    schema {
        query: QueryType
    }

    type QueryType {
        characters(idIn: [String!], nameIn: [String!]): [Character!]!
    }

    type Character {
        id: ID!
        name: String!
    }
""".trimIndent()

private val runtimeWiring = RuntimeWiring.newRuntimeWiring()
    .type(
        "QueryType"
    ) { typeWiring ->
        typeWiring
            .dataFetcher("characters", Data.CharactersFetcher)
    }
    .build()

data class Character(
    val id: Long,
    val name: String
)

private object Data {
    private val characters = setOf(
        Character(1L, "碇 シンジ"),
        Character(2L, "碇 ユイ"),
        Character(3L, "碇 ゲンドウ"),
        Character(4L, "綾波 レイ")
    )

    private class PredicateFactory<T>(
        val argumentName: String,
        val argumentType: Class<T>,
        val predicateCreator: (T) -> (Character) -> Boolean
    ) {
        fun createPredicate(argument: Any?) = predicateCreator(argumentType.cast(argument))
    }

    val predicateFactories = listOf(
        PredicateFactory("idIn", List::class.java) { ids -> { ids.contains(it.id.toString()) } },
        PredicateFactory("nameIn", List::class.java) { names -> { names.contains(it.name) } }
    )

    object CharactersFetcher : DataFetcher<List<Character>> {
        override fun get(environment: DataFetchingEnvironment): List<Character> {
            val arguments = environment.arguments

            val predicates = predicateFactories.mapNotNull { predicateFactory ->
                if (arguments.containsKey(predicateFactory.argumentName)) {
                    val argumentValue = arguments[predicateFactory.argumentName]
                    predicateFactory.createPredicate(argumentValue)
                } else {
                    null
                }
            }

            return predicates.fold(characters.toList()) { c, p -> c.filter(p) }
        }
    }
}
