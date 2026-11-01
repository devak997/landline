@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.deva.devphone.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.deva.devphone.data.HistoryType
import dev.deva.devphone.line.PhoneNumbers
import dev.deva.devphone.ui.components.CornerBadge
import dev.deva.devphone.ui.components.ExpressiveAvatar
import dev.deva.devphone.ui.components.Illustration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun RecentsPane(
    rows: List<RecentRow>,
    allowed: Boolean,
    onCall: (String) -> Unit,
    onGrant: () -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    when {
        !allowed -> {
            EmptyState(Icons.Rounded.Lock, "Allow call history", "Landline calls are kept in Android's own call log.", "Allow", onGrant)
        }

        rows.isEmpty() -> {
            EmptyState(Icons.Rounded.Call, "No landline calls yet", "Calls you make and receive on the landline show up here.", null, {})
        }

        else -> {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                var lastGroup: String? = null
                for (row in rows) {
                    val group = groupOf(row.dateMillis, now)
                    if (group != lastGroup) {
                        lastGroup = group
                        item(key = "h-$group") {
                            Text(
                                group,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp),
                            )
                        }
                    }
                    item(key = row.id) { RecentItem(row, onCall) }
                }
            }
        }
    }
}

@Composable
private fun RecentItem(
    row: RecentRow,
    onCall: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val missed = row.type == HistoryType.Missed || row.type == HistoryType.Rejected
    val title = row.name ?: PhoneNumbers.pretty(row.number)
    val (icon, word) =
        when (row.type) {
            HistoryType.Incoming -> Icons.AutoMirrored.Rounded.CallReceived to "Incoming"
            HistoryType.Outgoing -> Icons.AutoMirrored.Rounded.CallMade to "Outgoing"
            HistoryType.Missed -> Icons.AutoMirrored.Rounded.CallMissed to "Missed"
            HistoryType.Rejected -> Icons.Rounded.CallEnd to "Declined"
        }
    val (badgeBg, badgeFg) =
        when (row.type) {
            HistoryType.Missed, HistoryType.Rejected -> cs.errorContainer to cs.onErrorContainer
            HistoryType.Incoming -> cs.secondaryContainer to cs.onSecondaryContainer
            HistoryType.Outgoing -> cs.tertiaryContainer to cs.onTertiaryContainer
        }
    val time = DateUtils.formatDateTime(LocalContext.current, row.dateMillis, DateUtils.FORMAT_SHOW_TIME)
    val detail =
        buildString {
            append(word)
            append(" · ")
            append(time)
            if (row.durationSec > 0) {
                append(" · ")
                append(duration(row.durationSec))
            }
        }
    ListItem(
        colors = ListItemDefaults.colors(containerColor = cs.surface),
        leadingContent = {
            Box(Modifier.size(52.dp)) {
                ExpressiveAvatar(title, photo = row.photo, modifier = Modifier.align(Alignment.TopStart))
                CornerBadge(icon, badgeBg, badgeFg, ring = cs.surface)
            }
        },
        headlineContent = { Text(title, color = if (missed) cs.error else cs.onSurface, maxLines = 1) },
        supportingContent = {
            Text(detail, color = cs.onSurfaceVariant, maxLines = 1)
        },
        trailingContent = {
            FilledTonalIconButton(onClick = { onCall(row.number) }) {
                Icon(Icons.Rounded.Call, contentDescription = "Call $title")
            }
        },
    )
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    button: String?,
    onButton: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Illustration(icon)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (button != null) Button(onClick = onButton) { Text(button) }
        }
    }
}

internal fun duration(sec: Long): String = if (sec >= 3600) {
    "%d:%02d:%02d".format(sec / 3600, sec % 3600 / 60, sec % 60)
} else {
    "%d:%02d".format(sec / 60, sec % 60)
}

internal fun groupOf(
    millis: Long,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val diff =
        ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(millis).atZone(zone).toLocalDate(),
            Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),
        )
    return when (diff) {
        0L -> "Today"
        1L -> "Yesterday"
        in 2L..6L -> "This week"
        else -> "Earlier"
    }
}
