package dev.deva.devphone.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightScheme =
    lightColorScheme(
        primary = Color(0xFF00696B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF9CF1F2),
        onPrimaryContainer = Color(0xFF002021),
        secondary = Color(0xFF4A6363),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E7),
        onSecondaryContainer = Color(0xFF051F1F),
        tertiary = Color(0xFF9A4524),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDBCF),
        onTertiaryContainer = Color(0xFF380D00),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF4FBFA),
        onBackground = Color(0xFF161D1D),
        surface = Color(0xFFF4FBFA),
        onSurface = Color(0xFF161D1D),
        surfaceVariant = Color(0xFFDAE4E4),
        onSurfaceVariant = Color(0xFF3F4948),
        outline = Color(0xFF6F7979),
        outlineVariant = Color(0xFFBEC9C8),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEEF5F4),
        surfaceContainer = Color(0xFFE8EFEE),
        surfaceContainerHigh = Color(0xFFE3E9E9),
        surfaceContainerHighest = Color(0xFFDDE4E3),
        inverseSurface = Color(0xFF2B3231),
        inverseOnSurface = Color(0xFFECF2F1),
        inversePrimary = Color(0xFF80D4D6),
    )

val DarkScheme =
    darkColorScheme(
        primary = Color(0xFF80D4D6),
        onPrimary = Color(0xFF003738),
        primaryContainer = Color(0xFF004F51),
        onPrimaryContainer = Color(0xFF9CF1F2),
        secondary = Color(0xFFB0CCCB),
        onSecondary = Color(0xFF1B3435),
        secondaryContainer = Color(0xFF324B4B),
        onSecondaryContainer = Color(0xFFCCE8E7),
        tertiary = Color(0xFFFFB59A),
        onTertiary = Color(0xFF5B1B02),
        tertiaryContainer = Color(0xFF7B2E0F),
        onTertiaryContainer = Color(0xFFFFDBCF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E1515),
        onBackground = Color(0xFFDDE4E3),
        surface = Color(0xFF0E1515),
        onSurface = Color(0xFFDDE4E3),
        surfaceVariant = Color(0xFF3F4948),
        onSurfaceVariant = Color(0xFFBEC9C8),
        outline = Color(0xFF889392),
        outlineVariant = Color(0xFF3F4948),
        surfaceContainerLowest = Color(0xFF090F0F),
        surfaceContainerLow = Color(0xFF161D1D),
        surfaceContainer = Color(0xFF1A2121),
        surfaceContainerHigh = Color(0xFF252B2B),
        surfaceContainerHighest = Color(0xFF2F3636),
        inverseSurface = Color(0xFFDDE4E3),
        inverseOnSurface = Color(0xFF2B3231),
        inversePrimary = Color(0xFF00696B),
    )

data class CallColors(
    val answer: Color,
    val onAnswer: Color,
)

val LightCallColors = CallColors(answer = Color(0xFF146C2E), onAnswer = Color(0xFFFFFFFF))
val DarkCallColors = CallColors(answer = Color(0xFF7ADC8C), onAnswer = Color(0xFF00391A))
