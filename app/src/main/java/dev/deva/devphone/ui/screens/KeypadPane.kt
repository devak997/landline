@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.deva.devphone.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.line.PhoneNumbers
import dev.deva.devphone.ui.components.CuePill
import dev.deva.devphone.ui.components.DialKey

private val KEYS =
    listOf(
        "1" to "",
        "2" to "ABC",
        "3" to "DEF",
        "4" to "GHI",
        "5" to "JKL",
        "6" to "MNO",
        "7" to "PQRS",
        "8" to "TUV",
        "9" to "WXYZ",
        "*" to "",
        "0" to "+",
        "#" to "",
    )

@Composable
fun KeypadPane(
    state: HomeState,
    actions: HomeActions,
) {
    val canCall = state.dialed.isNotEmpty() && PhoneNumbers.toDialString(state.dialed) != null
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        LineCue(state.status)
        NumberDisplay(state.dialed, state.dialedName)
        Spacer(Modifier.height(20.dp))
        for (row in KEYS.chunked(3)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for ((digit, letters) in row) {
                    DialKey(
                        digit,
                        letters,
                        onClick = { actions.onDigit(digit[0]) },
                        onLongClick = if (digit == "0") ({ actions.onDigit('+') }) else null,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = actions.onPickContact, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Contacts, contentDescription = "Pick a contact")
            }
            CallButton(enabled = canCall && state.status == LineStatus.Ready, onClick = actions.onCall)
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(state.dialed.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    BackspaceKey(actions.onBackspace, actions.onClear)
                }
            }
        }
    }
}

@Composable
private fun NumberDisplay(
    dialed: String,
    name: String?,
) {
    val style =
        when {
            dialed.length > 14 -> MaterialTheme.typography.headlineLarge
            dialed.length > 10 -> MaterialTheme.typography.displaySmall
            else -> MaterialTheme.typography.displayMedium
        }
    Column(
        Modifier.fillMaxWidth().height(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (dialed.isEmpty()) "Enter a number" else dialed,
            style = if (dialed.isEmpty()) MaterialTheme.typography.titleLarge else style,
            color = if (dialed.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Dialled number ${dialed.ifEmpty { "empty" }}" },
        )
        Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            if (name != null) {
                CuePill(
                    Icons.Rounded.Person,
                    name,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun LineCue(status: LineStatus) {
    val cs = MaterialTheme.colorScheme
    val cue =
        when (status) {
            LineStatus.Ready -> null
            LineStatus.Away -> Triple(Icons.Rounded.WifiFind, "Calls work on your home Wi-Fi", false)
            LineStatus.Connecting -> Triple(Icons.Rounded.Sync, "Connecting to Airtel…", false)
            LineStatus.NeedsSetup -> Triple(Icons.Rounded.Password, "Add your SIP password in settings", false)
            is LineStatus.Failed -> Triple(Icons.Rounded.PhoneDisabled, status.reason, true)
        }
    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
        if (cue != null) {
            val (icon, text, bad) = cue
            CuePill(
                icon,
                text,
                if (bad) cs.errorContainer else cs.secondaryContainer,
                if (bad) cs.onErrorContainer else cs.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun CallButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 22.dp else 36.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "call")
    val width by animateDpAsState(if (pressed) 120.dp else 112.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "w")
    val cs = MaterialTheme.colorScheme
    Surface(
        color = if (enabled) cs.primary else cs.surfaceContainerHighest,
        contentColor = if (enabled) cs.onPrimary else cs.onSurfaceVariant,
        shape = RoundedCornerShape(corner),
        modifier =
            Modifier
                .size(width, 72.dp)
                .clip(RoundedCornerShape(corner))
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics { contentDescription = "Call" },
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Call, null, Modifier.size(32.dp)) }
    }
}

@Composable
private fun BackspaceKey(
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .combinedClickable(role = Role.Button, onClick = onBackspace, onLongClick = onClear)
            .semantics { contentDescription = "Delete" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
