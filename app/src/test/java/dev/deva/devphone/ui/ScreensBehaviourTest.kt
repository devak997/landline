package dev.deva.devphone.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.deva.devphone.line.LineConfig
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.ui.screens.CallActions
import dev.deva.devphone.ui.screens.CallScreen
import dev.deva.devphone.ui.screens.HomeActions
import dev.deva.devphone.ui.screens.HomeScreen
import dev.deva.devphone.ui.screens.RecentsPane
import dev.deva.devphone.ui.screens.Tab
import dev.deva.devphone.ui.screens.WelcomeScreen
import dev.deva.devphone.ui.theme.DevPhoneTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreensBehaviourTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `keypad sends digits and calls when ready`() {
        val typed = StringBuilder()
        var calls = 0
        rule.setContent {
            DevPhoneTheme(dynamicColor = false) {
                HomeScreen(Fixtures.home(), HomeActions(onDigit = { typed.append(it) }, onCall = { calls++ }))
            }
        }
        rule.onNodeWithContentDescription("5").performClick()
        rule.onNodeWithContentDescription("0").performClick()
        rule.onNodeWithContentDescription("#").performClick()
        assertEquals("50#", typed.toString())
        rule.onNodeWithContentDescription("Call").assertIsEnabled().performClick()
        assertEquals(1, calls)
        rule.onNodeWithText("Amma").assertExists()
    }

    @Test fun `call button is disabled while the line is not ready`() {
        rule.setContent {
            DevPhoneTheme(dynamicColor = false) { HomeScreen(Fixtures.home(status = LineStatus.Connecting), HomeActions()) }
        }
        rule.onNodeWithContentDescription("Call").assertIsNotEnabled()
    }

    @Test fun `call button is disabled for an undialable number`() {
        rule.setContent {
            DevPhoneTheme(dynamicColor = false) { HomeScreen(Fixtures.home(dialed = "12"), HomeActions()) }
        }
        rule.onNodeWithContentDescription("Call").assertIsNotEnabled()
    }

    @Test fun `touch targets are at least 48dp`() {
        rule.setContent { DevPhoneTheme(dynamicColor = false) { HomeScreen(Fixtures.home(), HomeActions()) } }
        listOf("1", "0", "Call", "Delete", "Pick a contact", "Settings").forEach {
            rule.onNodeWithContentDescription(it).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
    }

    @Test fun `recents call back the right number and flag missed calls`() {
        var called: String? = null
        rule.setContent {
            DevPhoneTheme(dynamicColor = false) {
                RecentsPane(Fixtures.recents, allowed = true, onCall = { called = it }, onGrant = {}, now = Fixtures.NOW)
            }
        }
        rule.onNodeWithContentDescription("Call Ravi Kumar").performClick()
        assertEquals("+919000012345", called)
        rule.onNodeWithText("Today").assertExists()
        rule.onNodeWithText("+91 402 345 6789").assertExists()
    }

    @Test fun `recents explain the missing permission`() {
        var asked = false
        rule.setContent { DevPhoneTheme(dynamicColor = false) { RecentsPane(emptyList(), false, {}, { asked = true }) } }
        rule.onNodeWithText("Allow").performClick()
        assertTrue(asked)
    }

    @Test fun `incoming call can be answered and declined`() {
        var answered = 0
        var declined = 0
        rule.setContent {
            DevPhoneTheme(dynamicColor = false) {
                CallScreen(Fixtures.incoming, CallActions(onAnswer = { answered++ }, onHangup = { declined++ }), { Fixtures.NOW })
            }
        }
        rule.onNodeWithContentDescription("Answer").assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithContentDescription("Decline").performClick()
        assertEquals(1, answered)
        assertEquals(1, declined)
        rule.onNodeWithText("Incoming landline call").assertExists()
    }

    @Test fun `active call shows timer and toggles`() {
        var muted = 0
        var speaker = 0
        var hung = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            DevPhoneTheme(dynamicColor = false) {
                CallScreen(
                    Fixtures.active,
                    CallActions(onMute = { muted++ }, onSpeaker = { speaker++ }, onHangup = { hung++ }),
                    { Fixtures.NOW },
                )
            }
        }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("3:12").assertExists()
        rule.onNodeWithContentDescription("Unmute").performClick()
        rule.onNodeWithContentDescription("Speaker").performClick()
        rule.onNodeWithContentDescription("Hang up").performClick()
        assertEquals(listOf(1, 1, 1), listOf(muted, speaker, hung))
    }

    @Test fun `welcome needs a password before connecting`() {
        var saved: LineConfig? = null
        rule.setContent {
            DevPhoneTheme(dynamicColor = false) {
                WelcomeScreen(LineConfig(number = "+919000000000"), LineStatus.NeedsSetup, submitted = false, onConnect = {
                    saved = it
                }, onAdvanced = {}, onEdit = {})
            }
        }
        rule.onNodeWithText("Connect").assertIsNotEnabled()
        rule.onNodeWithText("SIP password").performTextInput("hunter2")
        rule.onNodeWithText("Connect").assertIsEnabled().performClick()
        assertEquals("hunter2", saved?.password)
        assertEquals("+919000000000", saved?.number)
        assertTrue(saved?.isComplete == true)
    }

    @Test fun `welcome shows live registration progress and failures`() {
        rule.setContent {
            DevPhoneTheme(dynamicColor = false) {
                WelcomeScreen(Fixtures.config, LineStatus.Failed("Password rejected"), true, {}, {}, {})
            }
        }
        rule.onNodeWithText("Password rejected").assertExists()
        rule.onNodeWithText("Check details").assertExists()
    }

    @Test fun `bottom navigation switches tabs`() {
        var tab: Tab? = null
        rule.setContent { DevPhoneTheme(dynamicColor = false) { HomeScreen(Fixtures.home(), HomeActions(onTab = { tab = it })) } }
        rule.onNodeWithText("Recents").performClick()
        assertEquals(Tab.Recents, tab)
        assertTrue(rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().size == 1)
    }
}
