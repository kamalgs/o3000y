package dev.o3000y.ui.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class QueryRequest(val sql: String)

@Serializable
data class QueryResponse(
    val columns: List<String> = emptyList(),
    val rows: List<List<kotlinx.serialization.json.JsonElement>> = emptyList(),
    val rowCount: Int = 0,
    val elapsedMs: Long = 0,
)

val apiClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

suspend fun executeQuery(sql: String): QueryResponse {
    return apiClient.post("/api/v1/query") {
        contentType(ContentType.Application.Json)
        setBody(QueryRequest(sql))
    }.body()
}

suspend fun getServices(): List<String> {
    return apiClient.get("/api/v1/services").body()
}

suspend fun getDistinctValues(column: String): List<String> {
    val result = executeQuery("SELECT DISTINCT ${column}::VARCHAR AS v FROM spans ORDER BY 1 LIMIT 50")
    return result.rows.mapNotNull { row ->
        row.firstOrNull()?.let { el ->
            val s = el.toString().trim('"')
            s.ifEmpty { null }
        }
    }
}
