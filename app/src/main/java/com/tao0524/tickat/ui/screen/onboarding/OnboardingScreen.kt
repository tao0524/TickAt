package com.tao0524.tickat.ui.screen.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// イラスト専用カラー（ウィジェットの外観を表現するため常にダーク固定）
private val IllustSurface     = Color(0xFF2B2930)
private val IllustPrimaryText = Color(0xFFFFFFFF)
private val IllustSecondText  = Color(0xFF9E9E9E)
private val IllustAccent      = Color(0xFFBB86FC)

private data class OnboardingPage(val title: String, val subtitle: String)

private val pages = listOf(
    OnboardingPage(
        title    = "ホーム画面に、時刻だけ。",
        subtitle = "TickAtはホーム画面を静かに保ちます。\n表示するのは今の時刻だけ。"
    ),
    OnboardingPage(
        title    = "タップして、今日をチェック。",
        subtitle = "ウィジェットをタップすると、今日のスケジュール一覧と\n進み具合がすぐにわかります。"
    ),
    OnboardingPage(
        title    = "2つのタイプで、毎日をナビ。",
        subtitle = "ウィジェットに表示される「タイムブロック」と、\n時間にお知らせする「リマインダー」。"
    )
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = pages.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TextButton(
            onClick = onComplete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 8.dp)
        ) {
            Text(
                "スキップ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "illustration"
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (page) {
                        0    -> Page1Illustration()
                        1    -> Page2Illustration()
                        else -> Page3Illustration()
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "text"
            ) { page ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = pages[page].title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = pages[page].subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(totalPages) { index ->
                    Canvas(modifier = Modifier.size(7.dp)) {
                        drawCircle(
                            color = if (index == currentPage) IllustAccent
                            else Color(0xFF3A383F)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (currentPage < totalPages - 1) currentPage++
                    else onComplete()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (currentPage < totalPages - 1) "次へ" else "はじめる",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun Page1Illustration() {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = IllustSurface
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "14:32",
                    color = IllustPrimaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun Page2Illustration() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = IllustSurface
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("14:32", color = IllustPrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }

        Text("↓", color = IllustAccent, fontSize = 20.sp, fontWeight = FontWeight.Light)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = IllustSurface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(14.dp).background(Color(0xFFBB86FC), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                        Text("✓", color = IllustSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("朝のルーティン", color = IllustSecondText, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(14.dp).border(1.5.dp, IllustSecondText, RoundedCornerShape(3.dp)))
                    Text("ミーティング", color = IllustPrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun Page3Illustration() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeatureChip(label = "タイムブロック", color = Color(0xFFBB86FC))
        FeatureChip(label = "リマインダー", color = Color(0xFF81D4FA))
    }
}

@Composable
private fun FeatureChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = IllustSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = color)
            }
            Text(
                text = label,
                color = IllustPrimaryText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}