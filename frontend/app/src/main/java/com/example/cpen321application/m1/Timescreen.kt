package com.example.cpen321application.m1

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class TimerPhase { SETUP, RUNNING, SURPRISE }

@Composable
fun TimerScreen(onBack: () -> Unit) {
    var minutesText by rememberSaveable { mutableStateOf("0") }
    var secondsText by rememberSaveable { mutableStateOf("10") }
    var phase by remember { mutableStateOf(TimerPhase.SETUP) }
    var endAt by remember { mutableLongStateOf(0L) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var inputError by remember { mutableStateOf<String?>(null) }

    // Countdown based on the real clock, so it stays accurate even if frames are delayed
    LaunchedEffect(phase, endAt) {
        if (phase != TimerPhase.RUNNING) return@LaunchedEffect
        while (true) {
            val left = endAt - SystemClock.elapsedRealtime()
            if (left <= 0) {
                remainingMs = 0
                playAlarmTone()
                phase = TimerPhase.SURPRISE
                break
            }
            remainingMs = left
            delay(100)
        }
    }

    fun start() {
        val m = minutesText.trim().toIntOrNull()
        val s = secondsText.trim().toIntOrNull()
        inputError = when {
            m == null || s == null -> "Enter whole numbers for minutes and seconds."
            m < 0 || s < 0 -> "Minutes and seconds can't be negative."
            s > 59 -> "Seconds must be between 0 and 59."
            m > 999 -> "Minutes must be 999 or less."
            m == 0 && s == 0 -> "Set the timer to at least 1 second."
            else -> null
        }
        if (inputError != null) return
        endAt = SystemClock.elapsedRealtime() + (m!! * 60L + s!!) * 1000L
        phase = TimerPhase.RUNNING
    }

    ScreenScaffold(title = "Timer", onBack = onBack) {
        when (phase) {
            TimerPhase.SETUP -> {
                Spacer(Modifier.height(24.dp))
                Text("How long should the timer run?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Minutes", minutesText, { minutesText = it }, Modifier.weight(1f))
                    NumberField("Seconds", secondsText, { secondsText = it }, Modifier.weight(1f))
                }
                inputError?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { start() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Start timer", fontSize = 17.sp)
                }
            }

            TimerPhase.RUNNING -> {
                val totalSeconds = (remainingMs + 999) / 1000
                Spacer(Modifier.height(64.dp))
                Text(
                    "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text("A surprise is waiting when this reaches zero.")
                Spacer(Modifier.height(32.dp))
                OutlinedButton(onClick = { phase = TimerPhase.SETUP }) { Text("Cancel timer") }
            }

            TimerPhase.SURPRISE -> {
                Text("Time's up!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Be a F1 Driver and test your reflexex. Tap the box, wait for green, then tap as fast as you can.",
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                ReactionGame()
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = { phase = TimerPhase.SETUP }) { Text("Set another timer") }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.length <= 3 && new.all { it.isDigit() }) onChange(new) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

private enum class ReactionState { READY, WAITING, GO, RESULT, TOO_SOON }

@Composable
private fun ReactionGame() {
    var state by remember { mutableStateOf(ReactionState.READY) }
    var goAt by remember { mutableLongStateOf(0L) }
    var lastMs by remember { mutableLongStateOf(0L) }
    var bestMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state) {
        if (state == ReactionState.WAITING) {
            delay(Random.nextLong(1500, 4500))
            goAt = SystemClock.elapsedRealtime()
            state = ReactionState.GO
        }
    }

    val (background, headline, detail) = when (state) {
        ReactionState.READY -> Triple(Color(0xFF3949AB), "Tap to begin", "Wait for green, then tap")
        ReactionState.WAITING -> Triple(Color(0xFFC62828), "Wait…", "Don't tap yet")
        ReactionState.GO -> Triple(Color(0xFF2E7D32), "TAP!", "")
        ReactionState.RESULT -> Triple(Color(0xFF3949AB), "$lastMs ms", "${rating(lastMs)}  Tap to try again")
        ReactionState.TOO_SOON -> Triple(Color(0xFFEF6C00), "Too soon!", "Tap to try again")
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable {
                when (state) {
                    ReactionState.READY, ReactionState.RESULT, ReactionState.TOO_SOON ->
                        state = ReactionState.WAITING
                    ReactionState.WAITING -> state = ReactionState.TOO_SOON
                    ReactionState.GO -> {
                        lastMs = SystemClock.elapsedRealtime() - goAt
                        bestMs = minOf(bestMs ?: Long.MAX_VALUE, lastMs)
                        state = ReactionState.RESULT
                    }
                }
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(headline, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            if (detail.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(detail, color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }

    bestMs?.let {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Best this session:", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text("$it ms")
        }
    }
}

private fun rating(ms: Long): String = when {
    ms < 200 -> "Lightning fast ⚡"
    ms < 280 -> "Pro gamer reflexes 🎮"
    ms < 400 -> "Solid! 👍"
    else -> "Maybe more coffee? ☕"
}

private fun playAlarmTone() {
    try {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
        Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 900)
    } catch (_: RuntimeException) {
        // Some emulators have no audio device; the timer still works without sound
    }
}