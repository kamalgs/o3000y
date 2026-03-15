package dev.o3000y.ui.explore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.o3000y.ui.api.getDistinctValues
import dev.o3000y.ui.components.*
import dev.o3000y.ui.query.*
import dev.o3000y.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(state: ExploreState) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { state.run() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QueryPanel(state, onRun = { scope.launch { state.run() } })

        if (state.error.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Summary
        if (state.summaryValues.isNotEmpty()) {
            SummaryRow(state.summaryValues, state.generatedSQL)
        }

        // Chart
        if (state.chartSeries.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
                TimeChart(
                    series = state.chartSeries,
                    label = state.query.measures.joinToString(", ") { it.label },
                )
            }
        }

        // Breakdown table
        if (state.breakdownRows.isNotEmpty()) {
            BreakdownTable(
                rows = state.breakdownRows,
                groupLabel = state.query.groupBy,
                valueLabel = state.query.measures.firstOrNull()?.label ?: "value",
                onDrillDown = { key -> scope.launch { state.drillDown(key); state.run() } },
            )
        }

        if (state.loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun QueryPanel(state: ExploreState, onRun: () -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Measures
            MeasureChips(state)

            // Filters
            FilterChips(state)

            // Controls row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                LabeledDropdown(
                    "Group by",
                    listOf("" to "(none)") + GROUP_BY_COLUMNS.map { it to it },
                    state.query.groupBy,
                    { state.query = state.query.copy(groupBy = it) },
                )
                LabeledDropdown(
                    "Time range",
                    TIME_RANGES,
                    state.query.timeRange,
                    { state.query = state.query.copy(timeRange = it) },
                )
                LabeledDropdown(
                    "Interval",
                    INTERVALS,
                    state.query.interval,
                    { state.query = state.query.copy(interval = it) },
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onRun,
                    enabled = !state.loading,
                ) {
                    Text(if (state.loading) "Running…" else "Run Query", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MeasureChips(state: ExploreState) {
    var expanded by remember { mutableStateOf(false) }
    val available = MEASURE_OPTIONS.filter { opt -> state.query.measures.none { it.key == opt.key } }

    Column {
        Text(
            "VISUALIZE",
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            state.query.measures.forEach { m ->
                Tag(
                    text = m.label,
                    onRemove = if (state.query.measures.size > 1) ({ state.removeMeasure(m.key) }) else null,
                )
            }
            if (available.isNotEmpty()) {
                Box {
                    TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("+ add", fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        available.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label, fontSize = 13.sp) },
                                onClick = { state.addMeasure(opt); expanded = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(state: ExploreState) {
    var adding by remember { mutableStateOf(false) }
    var newCol by remember { mutableStateOf(FILTER_COLUMNS.first()) }
    var newOp by remember { mutableStateOf("=") }
    var newVal by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Column {
        Text(
            "WHERE",
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            state.query.filters.forEachIndexed { i, f ->
                Tag(
                    text = "${f.column} ${f.operator} ${f.value}",
                    onRemove = { state.removeFilter(i) },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (!adding) {
                TextButton(onClick = {
                    adding = true
                    newCol = FILTER_COLUMNS.first()
                    newOp = "="
                    newVal = ""
                    scope.launch { suggestions = try { getDistinctValues(newCol) } catch (_: Exception) { emptyList() } }
                }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("+ add filter", fontSize = 12.sp)
                }
            }
        }

        // Inline filter editor
        if (adding) {
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Column dropdown
                Box {
                    var colExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { colExpanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(newCol, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = colExpanded, onDismissRequest = { colExpanded = false }) {
                        FILTER_COLUMNS.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col, fontSize = 13.sp) },
                                onClick = {
                                    newCol = col; colExpanded = false; newVal = ""
                                    scope.launch { suggestions = try { getDistinctValues(col) } catch (_: Exception) { emptyList() } }
                                },
                            )
                        }
                    }
                }

                // Operator dropdown
                Box {
                    var opExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { opExpanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(newOp, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = opExpanded, onDismissRequest = { opExpanded = false }) {
                        OPERATORS.forEach { op ->
                            DropdownMenuItem(
                                text = { Text(op, fontSize = 13.sp) },
                                onClick = { newOp = op; opExpanded = false },
                            )
                        }
                    }
                }

                // Value with autocomplete
                Box {
                    var valExpanded by remember { mutableStateOf(false) }
                    val filtered = suggestions.filter { it.contains(newVal, ignoreCase = true) }.take(20)
                    OutlinedTextField(
                        value = newVal,
                        onValueChange = { newVal = it; valExpanded = filtered.isNotEmpty() },
                        placeholder = { Text("value", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.width(180.dp).height(40.dp),
                    )
                    DropdownMenu(expanded = valExpanded && filtered.isNotEmpty(), onDismissRequest = { valExpanded = false }) {
                        filtered.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v, fontSize = 13.sp) },
                                onClick = { newVal = v; valExpanded = false },
                            )
                        }
                    }
                }

                TextButton(onClick = {
                    if (newVal.isNotBlank()) {
                        state.addFilter(Filter(newCol, newOp, newVal))
                        adding = false
                    }
                }) { Text("Add", fontSize = 12.sp) }

                TextButton(onClick = { adding = false }) {
                    Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(values: List<Pair<String, String>>, sql: String) {
    var showSQL by remember { mutableStateOf(false) }

    Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                values.forEach { (label, value) ->
                    Column(Modifier.padding(end = 24.dp)) {
                        Text(label.uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, fontSize = 28.sp, fontFamily = FontFamily.Default)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showSQL = !showSQL }) {
                    Text(if (showSQL) "Hide SQL" else "Show SQL", fontSize = 12.sp)
                }
            }
            if (showSQL) {
                Surface(
                    color = Color(0xFF1E1E2E),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(
                        sql,
                        color = Color(0xFFA6E3A1),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownTable(
    rows: List<BreakdownRow>,
    groupLabel: String,
    valueLabel: String,
    onDrillDown: (String) -> Unit,
) {
    val maxVal = rows.maxOfOrNull { it.value } ?: 1.0

    Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
        Column {
            // Header
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(groupLabel, fontSize = 11.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(valueLabel, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(120.dp))
            }
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth().clickable { onDrillDown(row.key) }.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.key, fontSize = 13.sp, color = Indigo, modifier = Modifier.weight(1f))
                    Text(
                        formatNum(row.value),
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(100.dp),
                    )
                    Box(Modifier.width(120.dp).padding(start = 8.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth((row.value / maxVal).toFloat())
                                .height(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(IndigoLight)
                        )
                    }
                }
            }
        }
    }
}

private fun formatNum(v: Double): String {
    val l = v.toLong()
    if (v == l.toDouble()) return l.toString()
    return ((v * 100).toLong() / 100.0).toString()
}
