package dev.deva.devphone.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.ui.screens.CallActions
import dev.deva.devphone.ui.screens.CallScreen
import dev.deva.devphone.ui.screens.HomeActions
import dev.deva.devphone.ui.screens.HomeScreen
import dev.deva.devphone.ui.screens.SettingsActions
import dev.deva.devphone.ui.screens.SettingsScreen
import dev.deva.devphone.ui.screens.Tab
import dev.deva.devphone.ui.screens.WelcomeScreen
import dev.deva.devphone.ui.theme.DevPhoneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(
        name: String,
        dark: Boolean,
        dynamic: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        rule.mainClock.autoAdvance = false
        rule.setContent { DevPhoneTheme(darkTheme = dark, dynamicColor = dynamic) { content() } }
        rule.mainClock.advanceTimeBy(1_200)
        rule.onRoot().captureRoboImage("screenshots/$name-${if (dark) "dark" else "light"}${if (dynamic) "-dynamic" else ""}.png")
    }

    @Test fun welcomeLight() = shot("1-welcome", false) {
        WelcomeScreen(Fixtures.config.copy(password = ""), LineStatus.NeedsSetup, false, {}, {}, {})
    }

    @Test fun welcomeDark() = shot("1-welcome", true) {
        WelcomeScreen(Fixtures.config.copy(password = ""), LineStatus.NeedsSetup, false, {}, {}, {})
    }

    @Test fun registeringLight() = shot("2-registering", false) { WelcomeScreen(Fixtures.config, LineStatus.Away, true, {}, {}, {}) }

    @Test fun keypadLight() = shot("3-keypad", false) { HomeScreen(Fixtures.home(), HomeActions()) }

    @Test fun keypadDark() = shot("3-keypad", true) { HomeScreen(Fixtures.home(), HomeActions()) }

    @Test fun keypadDynamic() = shot("3-keypad", false, dynamic = true) { HomeScreen(Fixtures.home(), HomeActions()) }

    @Test fun keypadAwayDark() = shot(
        "4-keypad-away",
        true,
    ) { HomeScreen(Fixtures.home(status = LineStatus.Away, dialed = ""), HomeActions()) }

    @Test fun recentsLight() = shot("5-recents", false) { HomeScreen(Fixtures.home(Tab.Recents), HomeActions()) }

    @Test fun recentsDark() = shot("5-recents", true) { HomeScreen(Fixtures.home(Tab.Recents), HomeActions()) }

    @Test fun incomingLight() = shot("6-incoming", false) { CallScreen(Fixtures.incoming, CallActions(), { Fixtures.NOW }) }

    @Test fun incomingDark() = shot("6-incoming", true) { CallScreen(Fixtures.incoming, CallActions(), { Fixtures.NOW }) }

    @Test fun activeLight() = shot("7-active", false) { CallScreen(Fixtures.active, CallActions(), { Fixtures.NOW }) }

    @Test fun activeDark() = shot("7-active", true) { CallScreen(Fixtures.active, CallActions(), { Fixtures.NOW }) }

    @Test fun settingsLight() = shot("8-settings", false) {
        SettingsScreen(Fixtures.config, LineStatus.Ready, Fixtures.setup, SettingsActions())
    }

    @Test fun settingsDark() = shot(
        "8-settings",
        true,
    ) { SettingsScreen(Fixtures.config, LineStatus.Ready, Fixtures.setup, SettingsActions()) }
}
