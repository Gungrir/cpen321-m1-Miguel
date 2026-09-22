package com.example.cpen321application.m1

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

private const val GRID = 16
private const val NEW_IMAGE_GAP_MS = 3000L

@Composable
fun PixelArtScreen(onBack: () -> Unit) {
    val cells = remember { mutableStateListOf<Color>().apply { repeat(GRID * GRID) { add(Color.White) } } }
    var status by remember { mutableStateOf("Connecting…") }
    val gridLine = MaterialTheme.colorScheme.outlineVariant

    DisposableEffect(Unit) {
        val main = Handler(Looper.getMainLooper())
        var lastPixelAt = 0L

        val socket = ApiClient.ws.newWebSocket(
            Request.Builder().url(M1Config.PIXEL_WS_URL).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    main.post { status = "Live: waiting for pixels" }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val pixel = runCatching { JSONObject(text) }.getOrNull() ?: return
                    val x = pixel.optInt("x", -1)
                    val y = pixel.optInt("y", -1)
                    if (x !in 0 until GRID || y !in 0 until GRID) return
                    val color = parseHex(pixel.optString("color")) ?: return

                    // Compose state is updated on the main thread
                    main.post {
                        val now = System.currentTimeMillis()
                        if (now - lastPixelAt > NEW_IMAGE_GAP_MS) {
                            for (i in cells.indices) cells[i] = Color.White
                        }
                        lastPixelAt = now
                        cells[y * GRID + x] = color
                        status = "Live"
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    main.post { status = "Disconnected. Go back and reopen to reconnect." }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    main.post { status = "Connection failed: ${t.message ?: "unknown error"}" }
                }
            }
        )

        onDispose { socket.close(1000, "Screen closed") }
    }

    ScreenScaffold(title = "Live pixel art", onBack = onBack) {
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val cell = size.width / GRID
            cells.forEachIndexed { i, color ->
                drawRect(
                    color = color,
                    topLeft = Offset((i % GRID) * cell, (i / GRID) * cell),
                    size = Size(cell, cell)
                )
            }
            // faint grid lines so the blank canvas is visible
            for (k in 0..GRID) {
                val p = k * cell
                drawLine(gridLine, Offset(p, 0f), Offset(p, size.height), strokeWidth = 1f)
                drawLine(gridLine, Offset(0f, p), Offset(size.width, p), strokeWidth = 1f)
            }
        }
    }
}

private fun parseHex(raw: String): Color? {
    if (raw.isBlank()) return null
    val hex = if (raw.startsWith("#")) raw else "#$raw"
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}