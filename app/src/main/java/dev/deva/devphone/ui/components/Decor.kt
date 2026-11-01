@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.deva.devphone.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

@Composable
fun BoxScope.DecorShape(
    polygon: RoundedPolygon,
    size: Dp,
    x: Dp,
    y: Dp,
    color: Color,
    rotation: Float = 0f,
    align: Alignment = Alignment.TopStart,
) {
    Box(
        Modifier
            .align(align)
            .offset(x, y)
            .size(size)
            .rotate(rotation)
            .clip(polygon.toShape())
            .background(color),
    )
}

@Composable
fun RingingRings(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val t = rememberInfiniteTransition(label = "rings")
    val p = t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Restart), label = "p").value
    Canvas(modifier.size(size)) {
        val maxR = this.size.minDimension / 2
        for (i in 0 until 3) {
            val phase = (p + i / 3f) % 1f
            drawCircle(
                color.copy(alpha = (1f - phase) * 0.55f),
                radius = maxR * (0.55f + 0.45f * phase),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

@Composable
fun Illustration(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        DecorShape(MaterialShapes.Sunny, size * 0.92f, 0.dp, 0.dp, cs.secondaryContainer, 8f, Alignment.Center)
        DecorShape(MaterialShapes.Cookie9Sided, size * 0.62f, size * 0.06f, size * 0.04f, cs.primaryContainer, 0f, Alignment.Center)
        DecorShape(MaterialShapes.Circle, size * 0.2f, size * 0.34f, -(size * 0.32f), cs.tertiaryContainer, 0f, Alignment.Center)
        Icon(icon, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(size * 0.28f).offset(size * 0.06f, size * 0.04f))
    }
}

@Composable
fun CuePill(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(color = container, contentColor = content, shape = CircleShape, modifier = modifier) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BoxScope.CornerBadge(
    icon: ImageVector,
    container: Color,
    content: Color,
    ring: Color,
    size: Dp = 20.dp,
) {
    Box(
        Modifier
            .align(Alignment.BottomEnd)
            .size(size + 4.dp)
            .clip(CircleShape)
            .background(ring),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = content, modifier = Modifier.size(size * 0.66f))
        }
    }
}
