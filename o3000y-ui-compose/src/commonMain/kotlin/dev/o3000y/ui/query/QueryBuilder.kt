package dev.o3000y.ui.query

import kotlinx.serialization.Serializable

@Serializable
data class Filter(val column: String, val operator: String, val value: String)

@Serializable
data class MeasureSpec(val key: String, val label: String)

data class ExploreQuery(
    val measures: List<MeasureSpec> = listOf(MeasureSpec("count", "COUNT")),
    val filters: List<Filter> = emptyList(),
    val timeRange: String = "1h",
    val interval: String = "5m",
    val groupBy: String = "",
)

val MEASURE_OPTIONS = listOf(
    MeasureSpec("count", "COUNT"),
    MeasureSpec("count_distinct_traces", "COUNT(distinct traces)"),
    MeasureSpec("rate", "RATE (/s)"),
    MeasureSpec("avg:duration_us", "AVG(duration_us)"),
    MeasureSpec("sum:duration_us", "SUM(duration_us)"),
    MeasureSpec("p50:duration_us", "P50(duration_us)"),
    MeasureSpec("p90:duration_us", "P90(duration_us)"),
    MeasureSpec("p99:duration_us", "P99(duration_us)"),
    MeasureSpec("max:duration_us", "MAX(duration_us)"),
    MeasureSpec("min:duration_us", "MIN(duration_us)"),
)

val FILTER_COLUMNS = listOf(
    "service_name", "operation_name", "trace_id", "span_id",
    "status_code", "status_message", "span_kind", "duration_us",
)

val GROUP_BY_COLUMNS = listOf("service_name", "operation_name", "status_code", "span_kind")

val TIME_RANGES = listOf(
    "5m" to "5 min", "15m" to "15 min", "1h" to "1 hour",
    "6h" to "6 hours", "24h" to "24 hours", "7d" to "7 days", "30d" to "30 days",
)

val INTERVALS = listOf(
    "10s" to "10s", "30s" to "30s", "1m" to "1 min",
    "5m" to "5 min", "15m" to "15 min", "1h" to "1 hour", "1d" to "1 day",
)

val OPERATORS = listOf("=", "!=", ">", "<", ">=", "<=", "LIKE", "NOT LIKE")

// ── SQL generation ──

private fun parseMeasure(key: String): Pair<String, String> {
    val i = key.indexOf(':')
    return if (i < 0) key to "" else key.substring(0, i) to key.substring(i + 1)
}

private fun measureToSQL(key: String): String {
    val (agg, col) = parseMeasure(key)
    return when (agg) {
        "count" -> "COUNT(*)"
        "count_distinct_traces" -> "COUNT(DISTINCT trace_id)"
        "rate" -> "COUNT(*)"
        "avg" -> "AVG($col)"
        "sum" -> "SUM($col)"
        "p50" -> "PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY $col)"
        "p90" -> "PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY $col)"
        "p99" -> "PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY $col)"
        "max" -> "MAX($col)"
        "min" -> "MIN($col)"
        else -> "COUNT(*)"
    }
}

private val NUMERIC_COLUMNS = setOf("duration_us", "status_code", "span_kind")

private fun escape(v: String) = v.replace("'", "''")

private fun intervalSQL(s: String): String {
    val m = Regex("^(\\d+)([smhd])$").find(s) ?: return "'1 minute'"
    val (n, u) = m.destructured
    val unit = mapOf("s" to "second", "m" to "minute", "h" to "hour", "d" to "day")[u] ?: "minute"
    return "'$n $unit${if (n.toInt() > 1) "s" else ""}'"
}

private fun rangeSQL(s: String): String {
    val m = Regex("^(\\d+)([mhd])$").find(s) ?: return "'1 hour'"
    val (n, u) = m.destructured
    val unit = mapOf("m" to "minute", "h" to "hour", "d" to "day")[u] ?: "hour"
    return "'$n $unit${if (n.toInt() > 1) "s" else ""}'"
}

private fun toSeconds(s: String, units: Map<String, Int>): Int {
    val m = Regex("^(\\d+)([smhd])$").find(s) ?: return 60
    val (n, u) = m.destructured
    return n.toInt() * (units[u] ?: 60)
}

private fun intervalSeconds(s: String) = toSeconds(s, mapOf("s" to 1, "m" to 60, "h" to 3600, "d" to 86400))
private fun rangeSeconds(s: String) = toSeconds(s, mapOf("m" to 60, "h" to 3600, "d" to 86400))

private fun buildWhere(filters: List<Filter>, timeRange: String): String {
    val clauses = mutableListOf("start_time >= NOW() - INTERVAL ${rangeSQL(timeRange)}")
    for (f in filters) {
        if (f.column.isBlank() || f.value.isBlank()) continue
        val isNum = f.column in NUMERIC_COLUMNS && f.operator in listOf("=", "!=", ">", "<", ">=", "<=")
        clauses += if (isNum) "${f.column} ${f.operator} ${escape(f.value)}"
        else "${f.column} ${f.operator} '${escape(f.value)}'"
    }
    return clauses.joinToString("\n  AND ")
}

private fun selectExprs(measures: List<MeasureSpec>, divisor: Int): String =
    measures.joinToString(",\n  ") { m ->
        if (m.key == "rate") "ROUND(COUNT(*)::DOUBLE / $divisor, 2) AS \"${m.label}\""
        else "${measureToSQL(m.key)} AS \"${m.label}\""
    }

fun buildTimeSeriesSQL(q: ExploreQuery): String {
    val where = buildWhere(q.filters, q.timeRange)
    val sels = selectExprs(q.measures, intervalSeconds(q.interval))
    val bucket = "time_bucket(INTERVAL ${intervalSQL(q.interval)}, start_time) AS bucket"
    val groupBy = if (q.groupBy.isNotEmpty()) {
        "SELECT\n  $bucket,\n  ${q.groupBy} AS group_key,\n  $sels\nFROM spans\nWHERE $where\nGROUP BY bucket, group_key\nORDER BY bucket, group_key"
    } else {
        "SELECT\n  $bucket,\n  $sels\nFROM spans\nWHERE $where\nGROUP BY bucket\nORDER BY bucket"
    }
    return groupBy
}

fun buildSummarySQL(q: ExploreQuery): String {
    val where = buildWhere(q.filters, q.timeRange)
    val sels = selectExprs(q.measures, rangeSeconds(q.timeRange))
    return if (q.groupBy.isNotEmpty()) {
        "SELECT\n  ${q.groupBy} AS group_key,\n  $sels\nFROM spans\nWHERE $where\nGROUP BY group_key\nORDER BY \"${q.measures.first().label}\" DESC\nLIMIT 20"
    } else {
        "SELECT $sels\nFROM spans\nWHERE $where"
    }
}
