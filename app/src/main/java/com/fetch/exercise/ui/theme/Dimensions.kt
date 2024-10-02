package com.fetch.exercise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalDimensions = staticCompositionLocalOf<Dimensions> {
    error("No Dimensions provided")
}

val MaterialTheme.dimensions: Dimensions
    @Composable
    get() = LocalDimensions.current

val PhoneDimensions = Dimensions(padding_x1 = 16.dp)

// You can increase dimensions for tablets, Chromebooks, etc.

@Immutable
data class Dimensions(
    val padding_x1: Dp, // 16dp
    val padding_x1_25: Dp = padding_x1 * 1.25f, // 20dp
    val padding_x1_5: Dp = padding_x1 * 1.5f, // 24dp
    val padding_x1_75: Dp = padding_x1 * 1.75f, // 28dp
    val padding_x2: Dp = padding_x1 * 2, // 32dp
    val padding_x2_5: Dp = padding_x1 * 2.5f, // 40dp
    val padding_x3: Dp = padding_x1 * 3, // 48dp
    val padding_x3_5: Dp = padding_x1 * 3.5f, // 56dp
    val padding_x4: Dp = padding_x1 * 4f, // 64dp
    val padding_x6: Dp = padding_x1 * 6f, // 96dp

    val padding_x0_125: Dp = padding_x1 * 0.125f, // 2dp
    val padding_x0_25: Dp = padding_x1 * 0.25f, // 4dp
    val padding_x0_375: Dp = padding_x1 * 0.375f, // 6dp
    val padding_x0_5: Dp = padding_x1 * 0.5f, // 8dp
    val padding_x0_75: Dp = padding_x1 * 0.75f, // 12dp
)
