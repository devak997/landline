@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package dev.deva.devphone.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.deva.devphone.line.LineConfig
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.line.PhoneNumbers
import dev.deva.devphone.ui.components.CuePill
import dev.deva.devphone.ui.components.Illustration
import dev.deva.devphone.ui.components.StatusBadge
import dev.deva.devphone.ui.components.words

@Composable
fun WelcomeScreen(
    config: LineConfig,
    status: LineStatus,
    submitted: Boolean,
    onConnect: (LineConfig) -> Unit,
    onAdvanced: () -> Unit,
    onEdit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.surface)) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(40.dp))
                Illustration(Icons.Rounded.Call)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Your landline,\non your phone",
                    style = MaterialTheme.typography.displaySmall,
                    color = cs.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Airtel Fibernet fixed line over home Wi-Fi.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CuePill(Icons.Rounded.Wifi, "Wi-Fi", cs.surfaceContainerHigh, cs.onSurfaceVariant)
                    CuePill(Icons.Rounded.EnergySavingsLeaf, "Low power", cs.surfaceContainerHigh, cs.onSurfaceVariant)
                    CuePill(Icons.Rounded.Contacts, "Contacts", cs.surfaceContainerHigh, cs.onSurfaceVariant)
                }
                Spacer(Modifier.height(32.dp))
                AnimatedContent(submitted, label = "welcome") { busy ->
                    if (busy) Progress(status, onEdit) else Form(config, onConnect, onAdvanced)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun Form(
    config: LineConfig,
    onConnect: (LineConfig) -> Unit,
    onAdvanced: () -> Unit,
) {
    var number by remember { mutableStateOf(config.number) }
    var password by remember { mutableStateOf(config.password) }
    var reveal by remember { mutableStateOf(false) }
    val ready = number.isNotBlank() && password.isNotBlank()
    val submit = { if (ready) onConnect(config.copy(number = number.trim(), password = password, enabled = true)) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text("Landline number") },
            supportingText = { Text("In +91 format, as on your Airtel bill") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Phone, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("SIP password") },
            supportingText = { Text("From the VoIP page of Airtel's router") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
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
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = submit,
            enabled = ready,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(60.dp),
        ) {
            Text("Connect", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onAdvanced, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Server settings")
        }
    }
}

@Composable
private fun Progress(
    status: LineStatus,
    onEdit: () -> Unit,
) {
    val (title, detail) =
        when (status) {
            LineStatus.Away -> "Waiting for home Wi-Fi" to "Connect to your home network and the line registers by itself."
            else -> status.words()
        }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (status) {
            LineStatus.Connecting -> StatusBadge(status, size = 72.dp)
            LineStatus.Away -> Illustration(Icons.Rounded.WifiFind, size = 120.dp)
            LineStatus.Ready -> Illustration(Icons.Rounded.CheckCircle, size = 120.dp)
            else -> StatusBadge(status, size = 72.dp)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (status is LineStatus.Failed) {
            Button(onClick = onEdit, shapes = ButtonDefaults.shapes()) { Text("Check details") }
        }
    }
}

fun welcomeDone(config: LineConfig) = "Connected as ${PhoneNumbers.pretty(config.number)}"
