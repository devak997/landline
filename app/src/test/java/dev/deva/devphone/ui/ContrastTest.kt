package dev.deva.devphone.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot
import dev.deva.devphone.ui.screens.callWashTop
import dev.deva.devphone.ui.theme.CallColors
import dev.deva.devphone.ui.theme.DarkCallColors
import dev.deva.devphone.ui.theme.DarkScheme
import dev.deva.devphone.ui.theme.LightCallColors
import dev.deva.devphone.ui.theme.LightScheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@RunWith(AndroidJUnit4::class)
class ContrastTest {
    private fun lum(c: Color): Double {
        fun ch(v: Float) = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
    }

    fun ratio(
        a: Color,
        b: Color,
    ): Double {
        val l1 = lum(a)
        val l2 = lum(b)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    private data class Pair(
        val what: String,
        val fg: Color,
        val bg: Color,
        val min: Double,
    )

    private fun pairs(
        s: ColorScheme,
        call: CallColors,
    ) = listOf(
        Pair("text on surface", s.onSurface, s.surface, 4.5),
        Pair("secondary text on surface", s.onSurfaceVariant, s.surface, 4.5),
        Pair("group header (primary) on surface", s.primary, s.surface, 4.5),
        Pair("contact name under number (primary)", s.primary, s.surface, 4.5),
        Pair("missed call (error) on surface", s.error, s.surface, 4.5),
        Pair("digit on key", s.onSurface, s.surfaceContainerHigh, 4.5),
        Pair("letters on key", s.onSurfaceVariant, s.surfaceContainerHigh, 4.5),
        Pair("digit on pressed key", s.onPrimaryContainer, s.primaryContainer, 4.5),
        Pair("call icon", s.onPrimary, s.primary, 3.0),
        Pair("disabled call icon", s.onSurfaceVariant, s.surfaceContainerHighest, 3.0),
        Pair("avatar on primaryContainer", s.onPrimaryContainer, s.primaryContainer, 4.5),
        Pair("avatar on secondaryContainer", s.onSecondaryContainer, s.secondaryContainer, 4.5),
        Pair("avatar on tertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer, 4.5),
        Pair("error badge", s.onErrorContainer, s.errorContainer, 4.5),
        Pair("away badge", s.onSurfaceVariant, s.surfaceContainerHighest, 3.0),
        Pair("tonal button", s.onSecondaryContainer, s.secondaryContainer, 3.0),
        Pair("call label on wash top", s.onSurfaceVariant, callWashTop(s), 4.5),
        Pair("caller name on wash top", s.onSurface, callWashTop(s), 4.5),
        Pair("caller name on wash bottom", s.onSurface, s.surface, 4.5),
        Pair("number on wash", s.onSurfaceVariant, callWashTop(s), 4.5),
        Pair("button labels at bottom", s.onSurface, s.surface, 4.5),
        Pair("toggle icon", s.onSurfaceVariant, s.surfaceContainerHighest, 3.0),
        Pair("hang up icon", s.onError, s.error, 3.0),
        Pair("answer icon", call.onAnswer, call.answer, 3.0),
        Pair("answer button against surface", call.answer, s.surface, 3.0),
        Pair("decline button against surface", s.error, s.surface, 3.0),
        Pair("status cue pill", s.onSecondaryContainer, s.secondaryContainer, 4.5),
        Pair("failure cue pill", s.onErrorContainer, s.errorContainer, 4.5),
        Pair("contact pill", s.onPrimaryContainer, s.primaryContainer, 4.5),
        Pair("feature / landline pill", s.onSurfaceVariant, s.surfaceContainerHigh, 4.5),
        Pair("incoming badge", s.onSecondaryContainer, s.secondaryContainer, 3.0),
        Pair("outgoing badge", s.onTertiaryContainer, s.tertiaryContainer, 3.0),
        Pair("missed badge", s.onErrorContainer, s.errorContainer, 3.0),
        Pair("illustration icon", s.onPrimaryContainer, s.primaryContainer, 3.0),
        Pair("section icon", s.primary, s.surface, 3.0),
        Pair("field icon", s.onSurfaceVariant, s.surface, 3.0),
        Pair("card text", s.onSurface, s.surfaceContainerHigh, 4.5),
        Pair("card secondary text", s.onSurfaceVariant, s.surfaceContainerHigh, 4.5),
        Pair("field outline", s.outline, s.surface, 3.0),
    )

    private fun check(
        name: String,
        s: ColorScheme,
        call: CallColors,
    ) {
        val failures =
            pairs(s, call).mapNotNull { p ->
                val r = ratio(p.fg, p.bg)
                if (r + 1e-9 < p.min) "%s: %.2f < %.1f".format(p.what, r, p.min) else null
            }
        assertTrue("$name contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun `light fallback palette`() = check("light", LightScheme, LightCallColors)

    @Test fun `dark fallback palette`() = check("dark", DarkScheme, DarkCallColors)

    @Test fun `system dynamic palettes`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        check("dynamic light", dynamicLightColorScheme(ctx), LightCallColors)
        check("dynamic dark", dynamicDarkColorScheme(ctx), DarkCallColors)
    }

    @Test fun `material you palettes for any wallpaper`() {
        val seeds = (0 until 360 step 30).map { Hct.from(it.toDouble(), 48.0, 50.0) } + Hct.from(0.0, 4.0, 50.0)
        val failures = mutableListOf<String>()
        for (seed in seeds) {
            for (dark in listOf(false, true)) {
                val scheme = toCompose(SchemeTonalSpot(seed, dark, 0.0), dark)
                runCatching {
                    check(
                        "hue ${seed.hue.toInt()} ${if (dark) "dark" else "light"}",
                        scheme,
                        if (dark) DarkCallColors else LightCallColors,
                    )
                }.onFailure { failures += it.message.orEmpty() }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun toCompose(
        d: DynamicScheme,
        dark: Boolean,
    ): ColorScheme {
        val m = MaterialDynamicColors()

        fun c(dc: com.google.android.material.color.utilities.DynamicColor) = Color(dc.getArgb(d))
        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = c(m.primary()),
            onPrimary = c(m.onPrimary()),
            primaryContainer = c(m.primaryContainer()),
            onPrimaryContainer = c(m.onPrimaryContainer()),
            secondary = c(m.secondary()),
            onSecondary = c(m.onSecondary()),
            secondaryContainer = c(m.secondaryContainer()),
            onSecondaryContainer = c(m.onSecondaryContainer()),
            tertiary = c(m.tertiary()),
            onTertiary = c(m.onTertiary()),
            tertiaryContainer = c(m.tertiaryContainer()),
            onTertiaryContainer = c(m.onTertiaryContainer()),
            error = c(m.error()),
            onError = c(m.onError()),
            errorContainer = c(m.errorContainer()),
            onErrorContainer = c(m.onErrorContainer()),
            surface = c(m.surface()),
            onSurface = c(m.onSurface()),
            onSurfaceVariant = c(m.onSurfaceVariant()),
            surfaceContainerLowest = c(m.surfaceContainerLowest()),
            surfaceContainerHigh = c(m.surfaceContainerHigh()),
            surfaceContainerHighest = c(m.surfaceContainerHighest()),
            outline = c(m.outline()),
        )
    }

    @Test fun `ratio sanity`() {
        assertTrue(ratio(Color.Black, Color.White) in 20.9..21.1)
        assertTrue(ratio(Color.White, Color.White) in 0.99..1.01)
    }
}
