package com.bengalbytes.zenvo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bengalbytes.zenvo.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZenvoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSettings by remember { mutableStateOf(false) }

                    if (showSettings) {
                        SettingsScreen(onBack = { showSettings = false })
                    } else {
                        TimerScreen(onOpenSettings = { showSettings = true })
                    }
                }
            }
        }
    }
}

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Zenvo",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (uiState.streakDays > 0) {
                        Text(
                            text = "🔥 ${uiState.streakDays} day streak",
                            fontSize = 12.sp,
                            color = WarningAmber
                        )
                    }
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SolidSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Daily Progress ──
            DailyProgressCard(
                dailyMinutes = uiState.dailyFocusMinutes,
                goalMinutes = uiState.focusPreferences.dailyGoalMinutes,
                progress = uiState.dailyGoalProgress
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Timer Ring ──
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = uiState.progress,
                    animationSpec = tween(durationMillis = 300),
                    label = "progress"
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    // Track
                    drawArc(
                        color = SolidSurface,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = PrimaryAccent,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.remainingTime,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary,
                        letterSpacing = (-2).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.statusText,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // ── Control Buttons ──
            if (!uiState.isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleTimer(isCustom = false) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SolidSurface)
                    ) {
                        Text("15 MIN", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.toggleTimer(isCustom = true) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("CUSTOM", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("STOP SESSION", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Hadith Section ──
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hadith for Wisdom",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Hadith",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.refreshHadith() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.currentHadith.text,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "— ${uiState.currentHadith.reference}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var customMin by remember { mutableStateOf((uiState.customDuration / 60000).toString()) }
    var dailyGoal by remember { mutableStateOf(uiState.focusPreferences.dailyGoalMinutes.toString()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SolidSurface)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Zenvo Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            SettingsSection(title = "⏱ Timer Control") {
                SettingsTextField(
                    label = "Custom Duration (minutes)",
                    value = customMin,
                    onValueChange = { customMin = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsTextField(
                    label = "Daily Focus Goal (minutes)",
                    value = dailyGoal,
                    onValueChange = { dailyGoal = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.updateWidgetDuration(customMin.toIntOrNull() ?: 15)
                    viewModel.updateDailyGoal(dailyGoal.toIntOrNull() ?: 120)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text(text = "Save & Apply", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkBackground)
            }
        }
    }
}

@Composable
fun DailyProgressCard(dailyMinutes: Long, goalMinutes: Int, progress: Float) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = SolidSurface, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(4.dp.toPx()))
                    drawArc(color = SuccessGreen, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = Stroke(4.dp.toPx()))
                }
                Text(text = "${(progress * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Today's Focus Goal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = "${dailyMinutes}m of ${goalMinutes}m completed", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(vertical = 12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SettingsTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedBorderColor = PrimaryAccent,
            focusedLabelColor = PrimaryAccent,
            unfocusedLabelColor = TextSecondary
        )
    )
}
