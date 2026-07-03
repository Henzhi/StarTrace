package com.startrace.feature.fragment.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startrace.design.theme.StarColors

/**
 * 情绪条目
 */
data class MoodOption(
    val key: String,
    val emoji: String,
    val label: String,
    val color: Color
)

val MOOD_OPTIONS = listOf(
    MoodOption("excited",  "🤩", "激动", StarColors.MoodExcited),
    MoodOption("calm",     "😌", "平静", StarColors.MoodCalm),
    MoodOption("confused", "🤔", "困惑", StarColors.MoodConfused),
    MoodOption("amazed",   "😲", "惊叹", StarColors.MoodAmazed)
)

/**
 * 情绪四态选择器 — 可选
 */
@Composable
fun MoodPicker(
    selectedMood: String?,
    onMoodSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MOOD_OPTIONS.forEach { mood ->
            val isSelected = selectedMood == mood.key

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = 0.6f),
                label = "moodScale"
            )

            val bgAlpha by animateColorAsState(
                targetValue = if (isSelected) mood.color.copy(alpha = 0.25f) else Color.Transparent,
                label = "moodBg"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        onMoodSelected(if (isSelected) null else mood.key)
                    }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(bgAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = mood.emoji, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = mood.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) mood.color else StarColors.OnSurface.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun MoodPickerPreview() {
    Box(
        modifier = Modifier
            .background(StarColors.Background)
            .padding(16.dp)
    ) {
        MoodPicker(selectedMood = "excited", onMoodSelected = {})
    }
}
