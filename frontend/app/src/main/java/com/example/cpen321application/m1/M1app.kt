package com.example.cpen321application.m1

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

enum class M1Screen { HOME, LOGIN, PIXELS, TIMER }

@Composable
fun M1App() {
    var screen by rememberSaveable { mutableStateOf(M1Screen.HOME) }
    val goHome = { screen = M1Screen.HOME }

    BackHandler(enabled = screen != M1Screen.HOME, onBack = goHome)

    when (screen) {
        M1Screen.HOME -> HomeScreen(onOpen = { screen = it })
        M1Screen.LOGIN -> LoginScreen(onBack = goHome)
        M1Screen.PIXELS -> PixelArtScreen(onBack = goHome)
        M1Screen.TIMER -> TimerScreen(onBack = goHome)
    }
}

private val HomeBackground = Color(0xFFF4F1FB)
private val Ink = Color(0xFF1C1A24)
private val Ocean = Color(0xFF2F6FE4)
private val Berry = Color(0xFFC2185B)
private val Sunflower = Color(0xFFFFB300)
private val Mint = Color(0xFF26A69A)
private val Grape = Color(0xFF7E57C2)

@Composable
private fun HomeScreen(onOpen: (M1Screen) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            PixelMosaic()
            Spacer(Modifier.height(32.dp))

            FeatureCard(
                emoji = "🔐",
                title = "Sign in",
                description = "Log in with Google and see live server and device details",
                color = Ocean,
                contentColor = Color.White,
                onClick = { onOpen(M1Screen.LOGIN) }
            )
            Spacer(Modifier.height(16.dp))
            FeatureCard(
                emoji = "🎨",
                title = "Live pixel art",
                description = "Watch a picture paint itself, one pixel at a time",
                color = Berry,
                contentColor = Color.White,
                onClick = { onOpen(M1Screen.PIXELS) }
            )
            Spacer(Modifier.height(16.dp))
            FeatureCard(
                emoji = "⏱️",
                title = "Timer",
                description = "Count down to a surprise challenge",
                color = Sunflower,
                contentColor = Ink,
                onClick = { onOpen(M1Screen.TIMER) }
            )
        }
    }
}

@Composable
private fun PixelMosaic() {
    val columns = 12
    val rows = 5
    val palette = listOf(Ocean, Berry, Sunflower, Mint, Grape, Color.White)

    val cells = remember {
        val random = Random(321)
        List(columns * rows) { palette[random.nextInt(palette.size)] }
    }
    val revealOrder = remember { (0 until columns * rows).shuffled(Random(26)) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(columns / rows.toFloat())
    ) {
        val gap = 5.dp.toPx()
        val cell = (size.width - gap * (columns - 1)) / columns
        val shown = (progress.value * cells.size).toInt()
        val corner = CornerRadius(cell * 0.28f, cell * 0.28f)

        for (step in 0 until cells.size) {
            val index = revealOrder[step]
            val x = (index % columns) * (cell + gap)
            val y = (index / columns) * (cell + gap)
            val visible = step < shown
            drawRoundRect(
                color = if (visible) cells[index] else Ink.copy(alpha = 0.06f),
                topLeft = Offset(x, y),
                size = Size(cell, cell),
                cornerRadius = corner
            )
        }
    }
}

@Composable
private fun FeatureCard(
    emoji: String,
    title: String,
    description: String,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(contentColor.copy(alpha = 0.16f), CircleShape)
            ) {
                Text(emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(description, fontSize = 14.sp, lineHeight = 19.sp, color = contentColor.copy(alpha = 0.85f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}