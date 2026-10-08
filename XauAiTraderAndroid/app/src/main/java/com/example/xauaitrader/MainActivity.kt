package com.example.xauaitrader

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class BotSettings(
    val riskPercent: Float = 0.5f,
    val confidence: Float = 0.62f,
    val maxPositions: Int = 1,
    val slAtr: Float = 1.5f,
    val tpAtr: Float = 2.25f,
    val dailyLoss: Float = 2f,
    val cooldown: Int = 15,
    val spread: Float = 0.30f,
    val tradingEnabled: Boolean = true,
    val liveMode: Boolean = false
)

data class BotStatus(
    val connected: Boolean = false,
    val running: Boolean = false,
    val symbol: String = "XAUUSD",
    val timeframe: String = "M5",
    val price: String = "—",
    val signal: String = "WAIT",
    val confidence: String = "—",
    val entry: String = "—",
    val sl: String = "—",
    val tp1: String = "—",
    val tp2: String = "—",
    val balance: String = "—", val equity: String = "—", val dailyPnl: String = "—",
    val positions: Int = 0
)

object ApiConfig { const val API_BASE_URL = "https://YOUR-VPS-DOMAIN.example/api/" }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TraderApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("xau_ai_settings", Context.MODE_PRIVATE) }
    var status by remember { mutableStateOf(BotStatus()) }
    var server by remember { mutableStateOf(prefs.getString("server", ApiConfig.API_BASE_URL) ?: ApiConfig.API_BASE_URL) }
    var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
    var settings by remember { mutableStateOf(loadSettings(prefs)) }
    var showSettings by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("XAU AI Trader", fontWeight = FontWeight.Bold) },
                actions = { TextButton(onClick = { showSettings = !showSettings }) { Text("SETTINGS") } }
            )
        }) { padding ->
            if (showSettings) {
                SettingsScreen(
                    settings = settings,
                    server = server,
                    token = token,
                    onServer = { server = it },
                    onToken = { token = it },
                    onSettings = { settings = it },
                    onSave = {
                        prefs.edit()
                            .putString("server", server.trim())
                            .putString("token", token)
                            .apply()
                        saveSettings(prefs, settings)
                        showSettings = false
                    }
                )
            } else {
                Dashboard(
                    modifier = Modifier.padding(padding),
                    status = status,
                    settings = settings,
                    busy = busy,
                    message = message,
                    onRefresh = {
                        busy = true
                        message = "Connecting to API..."
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                loadDashboard(server, token)
                            }
                            if (result.success) {
                                status = result.status
                                message = "Connected"
                            } else {
                                status = status.copy(connected = false)
                                message = result.message
                            }
                            busy = false
                        }
                    },
                    onToggle = {
                        busy = true
                        message = "Sending bot command..."
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                if (status.running)
                                    postApi(server, token, "/bot/stop")
                                else
                                    postApi(server, token, "/bot/start")
                            }
                            if (result.first) {
                                status = status.copy(
                                    connected = true,
                                    running = !status.running
                                )
                                message = "Bot command sent"
                            } else {
                                message = result.second
                            }
                            busy = false
                        }
                    },
                    onEmergency = {
                        busy = true
                        message = "Sending stop command..."
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                postApi(server, token, "/bot/stop")
                            }
                            if (result.first) {
                                status = status.copy(
                                    connected = true,
                                    running = false,
                                    positions = 0
                                )
                                message = "Bot stopped"
                            } else {
                                message = result.second
                            }
                            busy = false
                        }
                    }
                )
            }
        }
    }
}


data class DashboardResult(
    val success: Boolean,
    val status: BotStatus,
    val message: String
)

fun loadDashboard(server: String, token: String): DashboardResult {
    return try {
        val base = server.trim().removeSuffix("/")
        val statusObj = JSONObject(getApi(base, token, "/status"))
        val signalObj = JSONObject(getApi(base, token, "/signal"))
        val accountObj = JSONObject(getApi(base, token, "/account"))

        val running = statusObj.optString("status", "stopped")
            .equals("running", ignoreCase = true)

        val confidence = signalObj.optDouble("confidence", 0.0)
        val confidenceText = if (confidence <= 1.0)
            String.format(Locale.US, "%.0f%%", confidence * 100)
        else
            String.format(Locale.US, "%.0f%%", confidence)

        DashboardResult(
            true,
            BotStatus(
                connected = true,
                running = running,
                symbol = statusObj.optString("symbol", "XAUUSD"),
                timeframe = statusObj.optString("timeframe", "M5"),
                price = numberText(signalObj.optDouble("entry", 0.0)),
                signal = signalObj.optString("signal", "WAIT"),
                confidence = confidenceText,
                entry = numberText(signalObj.optDouble("entry", 0.0)),
                sl = numberText(signalObj.optDouble("stop_loss", 0.0)),
                tp1 = numberText(signalObj.optDouble("take_profit_1", 0.0)),
                tp2 = numberText(signalObj.optDouble("take_profit_2", 0.0)),
                balance = numberText(accountObj.optDouble("balance", 0.0)),
                equity = numberText(accountObj.optDouble("equity", 0.0)),
                dailyPnl = numberText(accountObj.optDouble("profit", 0.0)),
                positions = 0
            ),
            "Connected"
        )
    } catch (e: Exception) {
        DashboardResult(
            false,
            BotStatus(),
            "Connection error: ${e.message ?: "Unknown error"}"
        )
    }
}

fun numberText(value: Double): String =
    if (value == 0.0) "—"
    else String.format(Locale.US, "%.2f", value)

fun getApi(server: String, token: String, path: String): String {
    val connection = URL(server.trim().removeSuffix("/") + path)
        .openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("Accept", "application/json")
        if (token.isNotBlank())
            connection.setRequestProperty("Authorization", "Bearer $token")
        if (connection.responseCode !in 200..299)
            throw Exception("HTTP ${connection.responseCode}")
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

fun postApi(server: String, token: String, path: String): Pair<Boolean, String> {
    return try {
        val connection = URL(server.trim().removeSuffix("/") + path)
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            if (token.isNotBlank())
                connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use { it.write("{}".toByteArray()) }
            if (connection.responseCode in 200..299)
                Pair(true, "OK")
            else
                Pair(false, "HTTP ${connection.responseCode}")
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        Pair(false, "Connection error: ${e.message ?: "Unknown error"}")
    }
}

private fun loadSettings(p: android.content.SharedPreferences) = BotSettings(
    p.getFloat("risk", 0.5f), p.getFloat("confidence", 0.62f), p.getInt("maxPositions", 1),
    p.getFloat("slAtr", 1.5f), p.getFloat("tpAtr", 2.25f), p.getFloat("dailyLoss", 2f),
    p.getInt("cooldown", 15), p.getFloat("spread", 0.30f), p.getBoolean("tradingEnabled", true), p.getBoolean("liveMode", false)
)

private fun saveSettings(p: android.content.SharedPreferences, s: BotSettings) = p.edit()
    .putFloat("risk", s.riskPercent).putFloat("confidence", s.confidence).putInt("maxPositions", s.maxPositions)
    .putFloat("slAtr", s.slAtr).putFloat("tpAtr", s.tpAtr).putFloat("dailyLoss", s.dailyLoss)
    .putInt("cooldown", s.cooldown).putFloat("spread", s.spread)
    .putBoolean("tradingEnabled", s.tradingEnabled).putBoolean("liveMode", s.liveMode).apply()

@Composable
fun Dashboard(modifier: Modifier, status: BotStatus, settings: BotSettings, busy: Boolean, message: String, onRefresh: () -> Unit, onToggle: () -> Unit, onEmergency: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column { Text("${status.symbol} • ${status.timeframe}", style = MaterialTheme.typography.titleLarge); Text(if (status.connected) "Server connected" else "Not connected")
                    if (message.isNotBlank()) {
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    } }
                Button(onClick = onRefresh, enabled = !busy) { Text("Refresh") }
            }
        }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("AI SIGNAL", style = MaterialTheme.typography.labelLarge); Text(status.signal, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold); Text("Confidence: ${status.confidence}"); Spacer(Modifier.height(8.dp)); Text("Price: ${status.price}") } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("TRADE PLAN", style = MaterialTheme.typography.labelLarge); PlanRow("Entry", status.entry); PlanRow("Stop Loss", status.sl); PlanRow("TP1", status.tp1); PlanRow("TP2", status.tp2) } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("ACTIVE SETTINGS", style = MaterialTheme.typography.labelLarge); PlanRow("Risk / trade", "${settings.riskPercent}%"); PlanRow("AI confidence", "${(settings.confidence * 100).toInt()}%"); PlanRow("Max positions", settings.maxPositions.toString()); PlanRow("SL / TP", "${settings.slAtr} / ${settings.tpAtr} ATR"); PlanRow("Daily loss limit", "${settings.dailyLoss}%"); PlanRow("Mode", if (settings.liveMode) "LIVE" else "DEMO") } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("ACCOUNT", style = MaterialTheme.typography.labelLarge); PlanRow("Balance", status.balance); PlanRow("Equity", status.equity); PlanRow("Daily P/L", status.dailyPnl); PlanRow("Bot positions", status.positions.toString()) } } }
        item { Button(onClick = onToggle, enabled = !busy && settings.tradingEnabled, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (status.running) "STOP BOT" else "START BOT") } }
        item { OutlinedButton(onClick = onEmergency, enabled = !busy && status.positions > 0, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("EMERGENCY CLOSE ALL") } }
        item { Text("Settings are saved on this phone. Broker credentials stay on the VPS.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable fun PlanRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.SemiBold) } }

@Composable
fun SettingsScreen(settings: BotSettings, server: String, token: String, onServer: (String) -> Unit, onToken: (String) -> Unit, onSettings: (BotSettings) -> Unit, onSave: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Connection", style = MaterialTheme.typography.headlineSmall) }
        item { OutlinedTextField(server, onServer, Modifier.fillMaxWidth(), label = { Text("VPS API HTTPS URL") }) }
        item { OutlinedTextField(token, onToken, Modifier.fillMaxWidth(), label = { Text("API token") }) }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { Text("Trading controls", style = MaterialTheme.typography.headlineSmall) }
        item { SettingSlider("Risk per trade", settings.riskPercent, 0.1f..5f, "%.1f%%") { onSettings(settings.copy(riskPercent = it)) } }
        item { SettingSlider("AI confidence minimum", settings.confidence, 0.50f..0.90f, "%.0f%%", scale = 100f) { onSettings(settings.copy(confidence = it)) } }
        item { SettingSlider("Stop Loss ATR", settings.slAtr, 0.5f..3f, "%.1f ATR") { onSettings(settings.copy(slAtr = it)) } }
        item { SettingSlider("Take Profit ATR", settings.tpAtr, 0.75f..5f, "%.2f ATR") { onSettings(settings.copy(tpAtr = it)) } }
        item { SettingSlider("Daily loss limit", settings.dailyLoss, 0.5f..10f, "%.1f%%") { onSettings(settings.copy(dailyLoss = it)) } }
        item { SettingSlider("Max spread", settings.spread, 0.05f..1f, "%.2f") { onSettings(settings.copy(spread = it)) } }
        item { Text("Max open positions: ${settings.maxPositions}") }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Allow trading"); Switch(settings.tradingEnabled, { onSettings(settings.copy(tradingEnabled = it)) }) } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Live mode"); Switch(settings.liveMode, { onSettings(settings.copy(liveMode = it)) }) } }
        item { Button(onClick = onSave, Modifier.fillMaxWidth()) { Text("SAVE SETTINGS") } }
        item { Text("Use DEMO mode first. Live mode should only be enabled after the VPS/MT5 connection has been tested.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun SettingSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, format: String, scale: Float = 1f, onChange: (Float) -> Unit) {
    Column { Text(String.format(Locale.US, format, value * scale), fontWeight = FontWeight.SemiBold); Text(label, style = MaterialTheme.typography.bodySmall); Slider(value = value, onValueChange = onChange, valueRange = range) }
}
