package dev.o3000y.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.o3000y.ui.api.getServices
import dev.o3000y.ui.explore.ExploreScreen
import dev.o3000y.ui.explore.rememberExploreState
import dev.o3000y.ui.query.Filter
import dev.o3000y.ui.theme.Indigo
import dev.o3000y.ui.theme.IndigoLight
import dev.o3000y.ui.theme.O3000yTheme
import kotlinx.coroutines.launch

enum class Screen { Explore, SQL }

@Composable
fun App() {
    O3000yTheme {
        val exploreState = rememberExploreState()
        var screen by remember { mutableStateOf(Screen.Explore) }
        val scope = rememberCoroutineScope()

        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Sidebar
            Sidebar(
                activeService = exploreState.query.filters
                    .firstOrNull { it.column == "service_name" && it.operator == "=" }?.value,
                onSelectService = { svc ->
                    val filters = exploreState.query.filters
                        .filter { !(it.column == "service_name" && it.operator == "=") }
                    val current = exploreState.query.filters
                        .firstOrNull { it.column == "service_name" && it.operator == "=" }?.value
                    exploreState.query = exploreState.query.copy(
                        filters = if (svc == current) filters else filters + Filter("service_name", "=", svc)
                    )
                    screen = Screen.Explore
                    scope.launch { exploreState.run() }
                },
            )

            // Main content
            Column(Modifier.fillMaxSize()) {
                // Header
                Surface(tonalElevation = 0.dp, shadowElevation = 1.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("o3000y", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        NavTab("Explore", screen == Screen.Explore) { screen = Screen.Explore }
                        NavTab("SQL", screen == Screen.SQL) { screen = Screen.SQL }
                    }
                }

                // Content
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                ) {
                    when (screen) {
                        Screen.Explore -> ExploreScreen(exploreState)
                        Screen.SQL -> SqlScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTab(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) IndigoLight else androidx.compose.ui.graphics.Color.Transparent
    val color = if (active) Indigo else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        label,
        fontSize = 13.sp,
        color = color,
        fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.Medium else null,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun Sidebar(activeService: String?, onSelectService: (String) -> Unit) {
    var services by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        services = try { getServices() } catch (_: Exception) { emptyList() }
    }

    Surface(
        modifier = Modifier.width(200.dp).fillMaxHeight(),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
            Text(
                "SERVICES",
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (services.isEmpty()) {
                Text("No services found", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            services.forEach { svc ->
                val isActive = svc == activeService
                val bg = if (isActive) IndigoLight else androidx.compose.ui.graphics.Color.Transparent
                val color = if (isActive) Indigo else MaterialTheme.colorScheme.onSurface
                Text(
                    svc,
                    fontSize = 13.sp,
                    color = color,
                    fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Medium else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .clickable { onSelectService(svc) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SqlScreen() {
    var sql by remember { mutableStateOf("SELECT * FROM spans ORDER BY start_time DESC LIMIT 100") }
    var result by remember { mutableStateOf<dev.o3000y.ui.api.QueryResponse?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("SQL QUERY", fontSize = 11.sp, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = sql,
                    onValueChange = { sql = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true; error = ""
                            try { result = dev.o3000y.ui.api.executeQuery(sql) }
                            catch (e: Exception) { error = e.message ?: "Error"; result = null }
                            finally { loading = false }
                        }
                    },
                    enabled = !loading,
                ) {
                    Text(if (loading) "Running…" else "Execute", fontSize = 13.sp)
                }
            }
        }

        if (error.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }

        result?.let { r ->
            Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
                Column {
                    Text(
                        "${r.rowCount} rows · ${r.elapsedMs} ms",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                    // Simple table
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        r.columns.forEach { col ->
                            Text(col, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        }
                    }
                    r.rows.take(100).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                            row.forEachIndexed { i, cell ->
                                Text(
                                    cell.toString().trim('"'),
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
