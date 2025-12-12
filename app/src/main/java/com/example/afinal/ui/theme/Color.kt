package com.example.afinal.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. NEUTRAL COLORS (Black, White, Gray)
// ==========================================
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Transparent = Color(0x00000000)

val Gray50 = Color(0xFFF8F9FA)  // Background (màu hơi xám rất nhẹ)
val Gray100 = Color(0xFFF5F5F5)
val Gray400 = Color(0xFFBDBDBD) // Unactive icon
val Gray600 = Color(0xFF757575) // Description
val Gray900 = Color(0xFF2D2D2D) // Title

// ==========================================
// 2. BRAND COLORS (Purple & Pink)
// ==========================================
// Purple Palette
val Purple50 = Color(0xFFF3E5F5) // Home selected / Box Stats
val Purple200 = Color(0xFFCE93D8)
val Purple500 = Color(0xFF9C27B0)
val Purple700 = Color(0xFF7B1FA2)
val DeepPurple500 = Color(0xFF673AB7)

// Pink Palette
val Pink500 = Color(0xFFE91E63)
val PinkA200 = Color(0xFFFF4081)

// ==========================================
// 3. ACCENT COLORS (Green, Teal, Orange)
// ==========================================
// Teal/Green Palette (Cho nút Map, Icon location)
val Teal50 = Color(0xFFE0F2F1)   // Box Stats
val Teal400 = Color(0xFF26A69A)
val TealA400 = Color(0xFF1DE9B6) // Accent
val Green500 = Color(0xFF00C853)

// Orange Palette (Trending, Warning)
val Orange50 = Color(0xFFFFF3E0) // Box Stats
val Orange500 = Color(0xFFFF9800)
val DeepOrange500 = Color(0xFFFF5722)

// ==========================================
// 4. PRE-DEFINED GRADIENTS
// ==========================================
object AppGradients {
    val Purple = Brush.horizontalGradient(
        colors = listOf(Purple500, DeepPurple500)
    )

    val Orange = Brush.linearGradient(
        colors = listOf(Orange500, DeepOrange500)
    )

    val Teal = Brush.linearGradient(
        colors = listOf(TealA400, Green500)
    )

    val Pink = Brush.linearGradient(
        colors = listOf(Pink500, PinkA200)
    )

    val Red = Brush.verticalGradient(
        colors = listOf(DeepPurple500, Pink500)
    )
}