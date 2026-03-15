package dev.o3000y.ui.explore

import androidx.compose.runtime.*
import dev.o3000y.ui.api.QueryResponse
import dev.o3000y.ui.api.executeQuery
import dev.o3000y.ui.query.*
import dev.o3000y.ui.theme.ChartColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*

data class ChartPoint(val bucket: String, val value: Double)
data class ChartSeries(val name: String, val data: List<ChartPoint>, val colorIndex: Int)
data class BreakdownRow(val key: String, val value: Double)

class ExploreState {
    var query by mutableStateOf(ExploreQuery())
    var loading by mutableStateOf(false)
    var error by mutableStateOf("")
    var summaryValues by mutableStateOf<List<Pair<String, String>>>(emptyList())
    var chartSeries by mutableStateOf<List<ChartSeries>>(emptyList())
    var breakdownRows by mutableStateOf<List<BreakdownRow>>(emptyList())
    var generatedSQL by mutableStateOf("")

    suspend fun run() {
        loading = true
        error = ""
        summaryValues = emptyList()
        chartSeries = emptyList()
        breakdownRows = emptyList()

        val tsSQL = buildTimeSeriesSQL(query)
        val sumSQL = buildSummarySQL(query)
        generatedSQL = tsSQL

        try {
            val (tsResult, sumResult) = coroutineScope {
                val ts = async { executeQuery(tsSQL) }
                val sum = async { executeQuery(sumSQL) }
                ts.await() to sum.await()
            }

            if (query.groupBy.isNotEmpty()) {
                chartSeries = parseGroupedSeries(tsResult, query.measures.first().label)
                breakdownRows = parseBreakdownRows(sumResult, query.measures.first().label)
                summaryValues = listOf("Groups" to breakdownRows.size.toString())
            } else {
                chartSeries = parseMultiMeasureSeries(tsResult, query)
                summaryValues = parseSummaryRow(sumResult, query)
            }
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    fun addMeasure(spec: MeasureSpec) {
        if (query.measures.none { it.key == spec.key }) {
            query = query.copy(measures = query.measures + spec)
        }
    }

    fun removeMeasure(key: String) {
        if (query.measures.size > 1) {
            query = query.copy(measures = query.measures.filter { it.key != key })
        }
    }

    fun addFilter(filter: Filter) {
        query = query.copy(filters = query.filters + filter)
    }

    fun removeFilter(index: Int) {
        query = query.copy(filters = query.filters.filterIndexed { i, _ -> i != index })
    }

    fun drillDown(groupKey: String) {
        query = query.copy(
            filters = query.filters + Filter(query.groupBy, "=", groupKey),
            groupBy = "",
        )
    }
}

@Composable
fun rememberExploreState(): ExploreState = remember { ExploreState() }

// ── parsers ──

private fun jsonToDouble(el: JsonElement): Double = when (el) {
    is JsonPrimitive -> el.doubleOrNull ?: 0.0
    else -> 0.0
}

private fun jsonToString(el: JsonElement): String = when (el) {
    is JsonPrimitive -> el.content
    is JsonNull -> ""
    else -> el.toString()
}

private fun parseMultiMeasureSeries(r: QueryResponse, q: ExploreQuery): List<ChartSeries> {
    val bi = r.columns.indexOf("bucket")
    if (bi < 0) return emptyList()
    return q.measures.mapIndexedNotNull { idx, m ->
        val ci = r.columns.indexOf(m.label)
        if (ci < 0) return@mapIndexedNotNull null
        ChartSeries(
            name = m.label,
            data = r.rows.map { row -> ChartPoint(jsonToString(row[bi]), jsonToDouble(row[ci])) },
            colorIndex = idx,
        )
    }
}

private fun parseSummaryRow(r: QueryResponse, q: ExploreQuery): List<Pair<String, String>> {
    if (r.rows.isEmpty()) return emptyList()
    val row = r.rows.first()
    return q.measures.mapNotNull { m ->
        val ci = r.columns.indexOf(m.label)
        if (ci < 0) return@mapNotNull null
        m.label to formatNum(jsonToDouble(row[ci]))
    }
}

private fun parseGroupedSeries(r: QueryResponse, measureLabel: String): List<ChartSeries> {
    val bi = r.columns.indexOf("bucket")
    val gi = r.columns.indexOf("group_key")
    val vi = r.columns.indexOf(measureLabel)
    if (bi < 0 || gi < 0 || vi < 0) return emptyList()

    val groups = mutableMapOf<String, MutableList<ChartPoint>>()
    for (row in r.rows) {
        val key = jsonToString(row[gi])
        groups.getOrPut(key) { mutableListOf() }
            .add(ChartPoint(jsonToString(row[bi]), jsonToDouble(row[vi])))
    }
    return groups.entries.take(ChartColors.size).mapIndexed { i, (name, data) ->
        ChartSeries(name, data, i)
    }
}

private fun parseBreakdownRows(r: QueryResponse, measureLabel: String): List<BreakdownRow> {
    val gi = r.columns.indexOf("group_key")
    val vi = r.columns.indexOf(measureLabel)
    if (gi < 0 || vi < 0) return emptyList()
    return r.rows.map { row -> BreakdownRow(jsonToString(row[gi]), jsonToDouble(row[vi])) }
}

private fun formatNum(v: Double): String {
    val l = v.toLong()
    if (v == l.toDouble()) return l.toString()
    return ((v * 100).toLong() / 100.0).toString()
}
