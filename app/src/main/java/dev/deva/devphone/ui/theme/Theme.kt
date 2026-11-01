package dev.deva.devphone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalCallColors = staticCompositionLocalOf { LightCallColors }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DevPhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme: ColorScheme =
        when {
            dynamicColor -> {
                val ctx = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            }

            darkTheme -> {
                DarkScheme
            }

            else -> {
                LightScheme
            }
        }
    CompositionLocalProvider(LocalCallColors provides if (darkTheme) DarkCallColors else LightCallColors) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            typography = AppTypography,
            content = content,
        )
    }
}
