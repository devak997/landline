@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.deva.devphone.ui.components

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import dev.deva.devphone.line.LineStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatusBadge(
    status: LineStatus,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val cs = MaterialTheme.colorScheme
    if (status == LineStatus.Connecting) {
        LoadingIndicator(modifier.size(size), color = cs.primary)
        return
    }
    val style =
        when (status) {
            LineStatus.Ready -> {
                BadgeStyle(MaterialShapes.Cookie9Sided, cs.primaryContainer, cs.onPrimaryContainer, Icons.Rounded.Call)
            }

            LineStatus.Away -> {
                BadgeStyle(MaterialShapes.Slanted, cs.surfaceContainerHighest, cs.onSurfaceVariant, Icons.Rounded.WifiOff)
            }

            LineStatus.NeedsSetup -> {
                BadgeStyle(
                    MaterialShapes.SoftBurst,
                    cs.tertiaryContainer,
                    cs.onTertiaryContainer,
                    Icons.Rounded.Password,
                )
            }

            is LineStatus.Failed -> {
                BadgeStyle(MaterialShapes.Burst, cs.errorContainer, cs.onErrorContainer, Icons.Rounded.PhoneDisabled)
            }

            LineStatus.Connecting -> {
                error("handled above")
            }
        }
    val bg by animateColorAsState(style.container, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "badge")
    Box(modifier.size(size).clip(style.polygon.toShape()).background(bg), contentAlignment = Alignment.Center) {
        Icon(style.icon, contentDescription = null, tint = style.content, modifier = Modifier.size(size * 0.46f))
    }
}

private data class BadgeStyle(
    val polygon: androidx.graphics.shapes.RoundedPolygon,
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val avatarPolygons: List<RoundedPolygon> =
    listOf(
        MaterialShapes.Cookie4Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Flower,
        MaterialShapes.Gem,
        MaterialShapes.Pentagon,
        MaterialShapes.Sunny,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Puffy,
    )

@Composable
fun ExpressiveAvatar(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    photo: Uri? = null,
    polygon: RoundedPolygon? = null,
    spin: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val cs = MaterialTheme.colorScheme
    val key = label.hashCode().let { if (it < 0) -it else it }
    val shape = (polygon ?: avatarPolygons[key % avatarPolygons.size]).toShape()
    val (bg, fg) =
        listOf(
            cs.primaryContainer to cs.onPrimaryContainer,
            cs.secondaryContainer to cs.onSecondaryContainer,
            cs.tertiaryContainer to cs.onTertiaryContainer,
        )[key % 3]
    val rotation =
        if (spin) {
            val t = rememberInfiniteTransition(label = "spin")
            t.animateFloat(0f, 360f, infiniteRepeatable(tween(16_000, easing = LinearEasing)), label = "deg").value
        } else {
            0f
        }
    val bitmap = rememberContactPhoto(photo)
    Box(
        modifier
            .size(size)
            .rotate(rotation)
            .clip(shape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().rotate(-rotation),
            )
        } else {
            val initial = label.firstOrNull { it.isLetter() }?.uppercase()
            if (icon != null || initial == null) {
                Icon(
                    icon ?: Icons.Rounded.Person,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(size * 0.46f).rotate(-rotation),
                )
            } else {
                Text(
                    initial,
                    color = fg,
                    style = if (size >= 96.dp) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge,
                    modifier = Modifier.rotate(-rotation),
                )
            }
        }
    }
}

@Composable
private fun rememberContactPhoto(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    val state by produceState<ImageBitmap?>(null, uri) {
        value =
            uri?.let {
                withContext(Dispatchers.IO) {
                    runCatching {
                        ImageDecoder
                            .decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { d, _, _ ->
                                d.setTargetSize(192, 192)
                            }.asImageBitmap()
                    }.getOrNull()
                }
            }
    }
    return state
}

@Composable
fun DialKey(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 76.dp,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) size * 0.28f else size / 2, MaterialTheme.motionScheme.fastSpatialSpec(), label = "corner")
    val bg by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primaryContainer else container,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "keybg",
    )
    val fg by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "keyfg",
    )
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onLongClick =
                    onLongClick?.let { lc ->
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            lc()
                        }
                    },
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onClick()
                },
            ).semantics { contentDescription = digit },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digit, style = MaterialTheme.typography.headlineMedium, color = fg)
            if (letters.isNotEmpty()) {
                Text(letters, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun LineStatus.words(): Pair<String, String> = when (this) {
    LineStatus.Ready -> "Ready" to "Your landline is connected"
    LineStatus.Connecting -> "Connecting" to "Registering with Airtel…"
    LineStatus.Away -> "Paused" to "Not on home Wi-Fi"
    LineStatus.NeedsSetup -> "Set up" to "Add your SIP password in settings"
    is LineStatus.Failed -> "Offline" to reason
}
