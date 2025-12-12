package com.example.afinal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Purple500,       // Màu chính (Brand color)
    onPrimary = White,         // Màu chữ trên nền primary
    secondary = Pink500,       // Màu phụ (Brand secondary)
    onSecondary = White,
    tertiary = TealA400,       // Màu nhấn (ví dụ cho các nút nhỏ)

    background = Gray50,       // Màu nền App (hơi xám nhẹ)
    onBackground = Gray900,    // Màu chữ chính trên nền App

    surface = White,           // Màu nền Card
    onSurface = Gray900,       // Màu chữ trên Card
    surfaceVariant = Gray100,  // Màu nền phụ
    onSurfaceVariant = Gray600 // Màu chữ phụ (description)
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    secondary = PinkA200,
    background = Black,
    surface = Gray900,
    onSurface = White
)

@Composable
fun FINALTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LightColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = Color.Transparent.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}