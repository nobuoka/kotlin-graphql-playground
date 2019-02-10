package info.vividcode.playground.graphql.ktor

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.idl.SchemaGenerator
import graphql.schema.StaticDataFetcher
import graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import graphql.schema.idl.SchemaParser
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resource
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.response.respondOutputStream
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.future.asDeferred

fun main() {
    val schema = "type Query{hello: String}"
    val runtimeWiring = newRuntimeWiring()
        .type("Query") { it.dataFetcher("hello", StaticDataFetcher("world")) }
        .build()

    val typeDefinitionRegistry = SchemaParser().parse(schema)
    val graphqlSchema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
    val graphql = GraphQL.newGraphQL(graphqlSchema).build()

    embeddedServer(Netty, 8080) {
        install(AutoHeadResponse)

        routing {
            get("/") {
                call.respondText("GraphiQL endpoint : /graphiql\nGraphQL endpoint : /graphql", ContentType.Text.Plain)
            }

            resource("/graphiql", "graphiql.html")

            route("/graphql") {
                setupGraphqlEndpoint { graphql.executeAsync(it).asDeferred().await() }
            }
        }
    }.start(wait = true)
}

/**
 * See : [Serving over HTTP | GraphQL](https://graphql.org/learn/serving-over-http/)
 */
fun Route.setupGraphqlEndpoint(graphqlExecutor: GraphqlExecutor) {
    val mapper = ObjectMapper().registerModule(KotlinModule())
    val contentTypeJson = ContentType.parse("application/json")
    val contentTypeGraphql = ContentType.parse("application/graphql")

    val execute: suspend PipelineContext<Unit, ApplicationCall>.(GraphqlHttpRequestContent) -> Unit = { requestObject ->
        val executionInput = ExecutionInput.newExecutionInput()
            .query(requestObject.query)
            .operationName(requestObject.operationName)
            .variables(requestObject.variables)
            .build()
        val executionResult = graphqlExecutor(executionInput)
        call.respondOutputStream(contentType = contentTypeJson) {
            mapper.writeValue(this, executionResult.toSpecification())
        }
    }

    get {
        val request = GraphqlHttpRequestContent(
            call.request.queryParameters["query"] ?: "",
            call.request.queryParameters["operationName"],
            call.request.queryParameters["variables"]?.let {
                mapper.readValue<Map<String, Any>>(it, object : TypeReference<Map<String, Any>>() {})
            }
        )
        execute(request)
    }

    post {
        when {
            contentTypeJson == call.request.contentType() -> {
                val requestContent = call.receive<String>()
                execute(mapper.readValue(requestContent, GraphqlHttpRequestContent::class.java))
            }
            contentTypeGraphql == call.request.contentType() ->
                execute(GraphqlHttpRequestContent(call.receive(), null, null))
            else -> {
                call.respondStatusText(HttpStatusCode.UnsupportedMediaType)
            }
        }
    }

    handle {
        call.respondStatusText(HttpStatusCode.MethodNotAllowed)
    }
}

suspend fun ApplicationCall.respondStatusText(status: HttpStatusCode) {
    respondText(
        contentType = ContentType.parse("text/plain; charset=utf-8"),
        status = HttpStatusCode.MethodNotAllowed,
        text = "${status.value} ${status.description}"
    )
}

typealias GraphqlExecutor = suspend (ExecutionInput) -> ExecutionResult

data class GraphqlHttpRequestContent(
    val query: String,
    val operationName: String?,
    val variables: Map<String, Any?>?
)
