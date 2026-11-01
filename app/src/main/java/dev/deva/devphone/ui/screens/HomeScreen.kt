@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package dev.deva.devphone.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.deva.devphone.data.HistoryType
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.line.PhoneNumbers
import dev.deva.devphone.ui.components.StatusBadge
import dev.deva.devphone.ui.components.words

enum class Tab { Keypad, Recents }

data class RecentRow(
    val id: Long,
    val number: String,
    val name: String?,
    val type: HistoryType,
    val dateMillis: Long,
    val durationSec: Long,
    val photo: Uri? = null,
)

data class HomeState(
    val status: LineStatus,
    val ownNumber: String,
    val tab: Tab = Tab.Keypad,
    val dialed: String = "",
    val dialedName: String? = null,
    val recents: List<RecentRow> = emptyList(),
    val recentsAllowed: Boolean = true,
)

class HomeActions(
    val onTab: (Tab) -> Unit = {},
    val onDigit: (Char) -> Unit = {},
    val onBackspace: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onCall: () -> Unit = {},
    val onCallNumber: (String) -> Unit = {},
    val onPickContact: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onGrantHistory: () -> Unit = {},
)

@Composable
fun HomeScreen(
    state: HomeState,
    actions: HomeActions,
    snackbar: SnackbarHostState = SnackbarHostState(),
) {
    val enterSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val exitSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = { Header(state.status, state.ownNumber, actions.onOpenSettings) },
            bottomBar = {
                ShortNavigationBar {
                    ShortNavigationBarItem(
                        selected = state.tab == Tab.Keypad,
                        onClick = { actions.onTab(Tab.Keypad) },
                        icon = { Icon(if (state.tab == Tab.Keypad) Icons.Filled.Dialpad else Icons.Outlined.Dialpad, null) },
                        label = { Text("Keypad") },
                    )
                    ShortNavigationBarItem(
                        selected = state.tab == Tab.Recents,
                        onClick = { actions.onTab(Tab.Recents) },
                        icon = { Icon(if (state.tab == Tab.Recents) Icons.Filled.WatchLater else Icons.Outlined.WatchLater, null) },
                        label = { Text("Recents") },
                    )
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = state.tab,
                transitionSpec = {
                    fadeIn(enterSpec) togetherWith fadeOut(exitSpec)
                },
                modifier = Modifier.fillMaxSize().padding(padding),
                label = "tab",
            ) { tab ->
                when (tab) {
                    Tab.Keypad -> KeypadPane(state, actions)
                    Tab.Recents -> RecentsPane(state.recents, state.recentsAllowed, actions.onCallNumber, actions.onGrantHistory)
                }
            }
        }
    }
}

@Composable
private fun Header(
    status: LineStatus,
    ownNumber: String,
    onSettings: () -> Unit,
) {
    val (title, detail) = status.words()
    Row(
        Modifier.fillMaxWidth().padding(PaddingValues(start = 20.dp, end = 8.dp, top = 48.dp, bottom = 8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusBadge(status)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Landline · $title", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (status == LineStatus.Ready) PhoneNumbers.pretty(ownNumber) else detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
    }
}
