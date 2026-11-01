@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.deva.devphone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.deva.devphone.line.LineConfig
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.ui.components.StatusBadge
import dev.deva.devphone.ui.components.words

data class Setup(
    val batteryUnrestricted: Boolean,
    val missingPermissions: List<String>,
    val engineVersion: String?,
)

class SettingsActions(
    val onBack: () -> Unit = {},
    val onSave: (LineConfig) -> Unit = {},
    val onBattery: () -> Unit = {},
    val onPermissions: () -> Unit = {},
)

@Composable
fun SettingsScreen(
    config: LineConfig,
    status: LineStatus,
    setup: Setup,
    actions: SettingsActions,
) {
    var draft by remember(config) { mutableStateOf(config) }
    var reveal by remember { mutableStateOf(false) }
    val changed = draft != config
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Landline settings") },
                navigationIcon = { IconButton(onClick = actions.onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(status)

            Section("Account", Icons.Rounded.ManageAccounts) {
                SwitchRow(Icons.Rounded.PowerSettingsNew, "Line enabled", null, draft.enabled) { draft = draft.copy(enabled = it) }
                Field("Phone number", draft.number, KeyboardType.Phone, Icons.Rounded.Phone) { draft = draft.copy(number = it) }
                OutlinedTextField(
                    value = draft.password,
                    onValueChange = { draft = draft.copy(password = it) },
                    label = { Text("SIP password") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { reveal = !reveal }) {
                            Icon(
                                if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                if (reveal) "Hide password" else "Show password",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Field("IMS domain", draft.domain, KeyboardType.Uri, Icons.Rounded.Language) { draft = draft.copy(domain = it) }
                Field("SIP server", draft.proxy, KeyboardType.Uri, Icons.Rounded.Dns) { draft = draft.copy(proxy = it) }
                Field("Backup SIP server", draft.backupProxy, KeyboardType.Uri, Icons.Rounded.SettingsBackupRestore) {
                    draft =
                        draft.copy(backupProxy = it)
                }
                Button(
                    onClick = { actions.onSave(draft) },
                    enabled = changed,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Save and reconnect") }
            }

            Section("Reliability", Icons.Rounded.VerifiedUser) {
                SwitchRow(
                    Icons.Rounded.MonitorHeart,
                    "Keep-alive packets",
                    "Only needed if the router doesn't forward Airtel's calls to this phone. Uses a little more battery.",
                    draft.keepAlive,
                ) { draft = draft.copy(keepAlive = it) }
                InfoRow(
                    if (setup.batteryUnrestricted) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryAlert,
                    if (setup.batteryUnrestricted) {
                        "Battery: unrestricted. Android won't freeze the line in deep sleep."
                    } else {
                        "Battery: optimised. Android may delay incoming calls while the phone sleeps."
                    },
                )
                if (!setup.batteryUnrestricted) FilledTonalButton(onClick = actions.onBattery) { Text("Allow unrestricted battery") }
                if (setup.missingPermissions.isNotEmpty()) {
                    Text(
                        "Missing: ${setup.missingPermissions.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    FilledTonalButton(onClick = actions.onPermissions) { Text("Grant permissions") }
                }
            }

            Section("How it saves power", Icons.Rounded.EnergySavingsLeaf) {
                Text(
                    "The SIP engine only runs on your home Wi-Fi and stops completely elsewhere. " +
                        "With keep-alives off, the only periodic traffic is the hourly registration Airtel asks for.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                setup.engineVersion?.let {
                    Text("Linphone $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusCard(status: LineStatus) {
    val (title, detail) = status.words()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(status, size = 64.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        content()
    }
}

@Composable
private fun SwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    type: KeyboardType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = { Icon(icon, null) },
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = Modifier.fillMaxWidth(),
    )
}
