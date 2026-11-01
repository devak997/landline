@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.deva.devphone.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.deva.devphone.line.ActiveCall
import dev.deva.devphone.line.CallPhase
import dev.deva.devphone.line.Direction
import dev.deva.devphone.line.PhoneNumbers
import dev.deva.devphone.ui.components.CuePill
import dev.deva.devphone.ui.components.DialKey
import dev.deva.devphone.ui.components.ExpressiveAvatar
import dev.deva.devphone.ui.components.RingingRings
import dev.deva.devphone.ui.theme.LocalCallColors
import kotlinx.coroutines.delay

fun callWashTop(cs: androidx.compose.material3.ColorScheme) = androidx.compose.ui.graphics
    .lerp(cs.surface, cs.primaryContainer, CALL_WASH)

const val CALL_WASH = 0.45f

class CallActions(
    val onAnswer: () -> Unit = {},
    val onHangup: () -> Unit = {},
    val onMute: () -> Unit = {},
    val onSpeaker: () -> Unit = {},
    val onDtmf: (Char) -> Unit = {},
)

@Composable
fun CallScreen(
    call: ActiveCall,
    actions: CallActions,
    nowProvider: () -> Long = System::currentTimeMillis,
) {
    val cs = MaterialTheme.colorScheme
    val ringingIn = call.direction == Direction.Incoming && call.phase == CallPhase.Ringing
    val title = call.name ?: PhoneNumbers.pretty(call.number)
    var showPad by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(callWashTop(cs), cs.surface)))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))
            CuePill(Icons.Rounded.Phone, "Landline", cs.surfaceContainerHigh, cs.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Text(phaseLabel(call, nowProvider), style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            val pulse =
                if (ringingIn || call.phase == CallPhase.Dialing || call.phase == CallPhase.Ringing) {
                    rememberInfiniteTransition(label = "pulse")
                        .animateFloat(1f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
                        .value
                } else {
                    1f
                }
            val ringing = ringingIn || call.phase == CallPhase.Dialing || call.phase == CallPhase.Ringing
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                if (ringing) RingingRings(240.dp, cs.primary)
                ExpressiveAvatar(
                    title,
                    size = 168.dp,
                    photo = call.photo,
                    polygon = MaterialShapes.Cookie12Sided,
                    spin = ringing,
                    modifier = Modifier.scale(pulse),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, color = cs.onSurface, maxLines = 2)
            if (call.name != null) {
                Text(PhoneNumbers.pretty(call.number), style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(ringingIn, label = "controls") { incoming ->
                if (incoming) IncomingControls(actions) else InCallControls(call, actions, showPad) { showPad = !showPad }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun phaseLabel(
    call: ActiveCall,
    now: () -> Long,
): String {
    var tick by remember { mutableLongStateOf(now()) }
    if (call.phase == CallPhase.Active) {
        LaunchedEffect(call.connectedAt) {
            while (true) {
                tick = now()
                delay(1_000)
            }
        }
    }
    return when (call.phase) {
        CallPhase.Ringing -> if (call.direction == Direction.Incoming) "Incoming landline call" else "Ringing…"
        CallPhase.Dialing -> "Calling…"
        CallPhase.Connecting -> "Connecting…"
        CallPhase.Active -> duration(((tick - (call.connectedAt ?: tick)) / 1000).coerceAtLeast(0))
        CallPhase.Ended -> call.endReason ?: "Call ended"
    }
}

@Composable
private fun IncomingControls(actions: CallActions) {
    val cs = MaterialTheme.colorScheme
    val green = LocalCallColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        BigAction("Decline", Icons.Rounded.CallEnd, cs.error, cs.onError, actions.onHangup)
        BigAction("Answer", Icons.Rounded.Call, green.answer, green.onAnswer, actions.onAnswer)
    }
}

@Composable
private fun BigAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
            modifier = Modifier.size(width = 120.dp, height = 88.dp),
        ) { Icon(icon, contentDescription = label, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun InCallControls(
    call: ActiveCall,
    actions: CallActions,
    showPad: Boolean,
    onTogglePad: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showPad) {
            for (row in listOf("123", "456", "789", "*0#")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (d in row) {
                        DialKey(
                            d.toString(),
                            "",
                            onClick = { actions.onDtmf(d) },
                            size = 64.dp,
                            container = cs.surfaceContainerHighest,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Toggle(
                call.muted,
                if (call.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                if (call.muted) "Unmute" else "Mute",
                actions.onMute,
            )
            Toggle(showPad, Icons.Rounded.Dialpad, "Keypad", onTogglePad)
            Toggle(call.speaker, Icons.AutoMirrored.Rounded.VolumeUp, "Speaker", actions.onSpeaker)
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = actions.onHangup,
            enabled = call.phase != CallPhase.Ended,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError),
            modifier = Modifier.fillMaxWidth(0.7f).height(80.dp),
        ) { Icon(Icons.Rounded.CallEnd, contentDescription = "Hang up", modifier = Modifier.size(36.dp)) }
    }
}

@Composable
private fun Toggle(
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconToggleButton(
            checked = checked,
            onCheckedChange = { onClick() },
            shapes = IconButtonDefaults.toggleableShapes(),
            modifier = Modifier.size(76.dp),
        ) { Icon(icon, contentDescription = label, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
