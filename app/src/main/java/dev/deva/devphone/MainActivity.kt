package dev.deva.devphone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.service.LineService
import dev.deva.devphone.ui.screens.HomeActions
import dev.deva.devphone.ui.screens.HomeScreen
import dev.deva.devphone.ui.screens.HomeState
import dev.deva.devphone.ui.screens.RecentRow
import dev.deva.devphone.ui.screens.SettingsActions
import dev.deva.devphone.ui.screens.SettingsScreen
import dev.deva.devphone.ui.screens.Setup
import dev.deva.devphone.ui.screens.Tab
import dev.deva.devphone.ui.screens.WelcomeScreen
import dev.deva.devphone.ui.screens.welcomeDone
import dev.deva.devphone.ui.theme.DevPhoneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var incomingNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingNumber = dialFromIntent(intent)
        LineService.start(this)
        setContent { DevPhoneTheme { App() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dialFromIntent(intent)?.let { incomingNumber = it }
    }

    private fun dialFromIntent(intent: Intent?): String? = intent
        ?.data
        ?.takeIf { it.scheme == "tel" }
        ?.schemeSpecificPart
        ?.let(Uri::decode)

    @Composable
    private fun App() {
        val line = line
        val status by line.status.collectAsStateWithLifecycle()
        val config by line.configStore.config.collectAsStateWithLifecycle()
        val call by line.call.collectAsStateWithLifecycle()
        val historyVersion by line.historyVersion.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }

        var tab by rememberSaveable { mutableStateOf(Tab.Keypad) }
        var dialed by rememberSaveable { mutableStateOf("") }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var permissionsVersion by remember { mutableIntStateOf(0) }

        var onboarding by rememberSaveable { mutableStateOf(!config.isComplete) }
        var submitted by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(status, submitted) {
            if (onboarding && submitted && status == LineStatus.Ready) {
                onboarding = false
                snackbar.showSnackbar(welcomeDone(config))
            }
        }

        incomingNumber?.let {
            dialed = it.filter { c -> c.isDigit() || c == '+' }
            tab = Tab.Keypad
            incomingNumber = null
        }

        val permissions =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                line.contacts.invalidate()
                permissionsVersion++
            }
        LaunchedEffect(Unit) {
            val missing = missingPermissions()
            if (missing.isNotEmpty()) permissions.launch(missing.toTypedArray())
        }
        LifecycleResumeEffect(Unit) {
            permissionsVersion++
            onPauseOrDispose {}
        }

        val pickContact =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                val uri = res.data?.data ?: return@rememberLauncherForActivityResult
                contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        dialed = c.getString(0).filter { it.isDigit() || it == '+' }
                        tab = Tab.Keypad
                    }
                }
            }

        LaunchedEffect(call != null) {
            if (call != null) startActivity(Intent(this@MainActivity, CallActivity::class.java))
        }

        val recents by produceState(emptyList<RecentRow>(), historyVersion, permissionsVersion, tab) {
            value =
                withContext(Dispatchers.IO) {
                    line.history.load().map {
                        val contact = line.contacts.lookup(it.number)
                        RecentRow(it.id, it.number, contact?.name ?: it.name, it.type, it.dateMillis, it.durationSec, contact?.photo)
                    }
                }
        }
        val dialedName by produceState<String?>(null, dialed, permissionsVersion) {
            delay(250)
            value = if (dialed.length >= 6) withContext(Dispatchers.IO) { line.contacts.lookup(dialed)?.name } else null
        }

        fun call(number: String) {
            val error = line.dial(number)
            if (error != null) scope.launch { snackbar.showSnackbar(error) } else dialed = ""
        }

        BackHandler(showSettings) { showSettings = false }
        AnimatedContent(showSettings to onboarding, label = "route") { (settings, welcome) ->
            if (welcome && !settings) {
                WelcomeScreen(
                    config,
                    status,
                    submitted,
                    onConnect = {
                        line.configStore.save(it)
                        line.onConfigChanged()
                        submitted = true
                    },
                    onAdvanced = { showSettings = true },
                    onEdit = { submitted = false },
                )
            } else if (settings) {
                val setup =
                    Setup(
                        batteryUnrestricted = remember(permissionsVersion) { isBatteryUnrestricted() },
                        missingPermissions = remember(permissionsVersion) { missingPermissions().map(::friendlyName) },
                        engineVersion = line.engineVersion,
                    )
                SettingsScreen(
                    config,
                    status,
                    setup,
                    SettingsActions(
                        onBack = { showSettings = false },
                        onSave = {
                            line.configStore.save(it)
                            line.onConfigChanged()
                            showSettings = false
                            if (onboarding) {
                                submitted =
                                    it.isComplete
                            }
                        },
                        onBattery = {
                            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:$packageName".toUri()))
                        },
                        onPermissions = { permissions.launch(missingPermissions().toTypedArray()) },
                    ),
                )
            } else {
                HomeScreen(
                    HomeState(status, config.number, tab, dialed, dialedName, recents, line.history.canRead()),
                    HomeActions(
                        onTab = { tab = it },
                        onDigit = { if (dialed.length < 20) dialed += it },
                        onBackspace = { dialed = dialed.dropLast(1) },
                        onClear = { dialed = "" },
                        onCall = { call(dialed) },
                        onCallNumber = { call(it) },
                        onPickContact = {
                            pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                        },
                        onOpenSettings = { showSettings = true },
                        onGrantHistory = { permissions.launch(missingPermissions().toTypedArray()) },
                    ),
                    snackbar,
                )
            }
        }
    }

    private fun isBatteryUnrestricted() = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    private fun missingPermissions(): List<String> = RUNTIME_PERMISSIONS.filter {
        checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }

    private fun friendlyName(p: String) = when (p) {
        Manifest.permission.RECORD_AUDIO -> "microphone"
        Manifest.permission.POST_NOTIFICATIONS -> "notifications"
        Manifest.permission.READ_CONTACTS -> "contacts"
        Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG -> "call log"
        Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth"
        else -> p.substringAfterLast('.')
    }

    companion object {
        val RUNTIME_PERMISSIONS =
            listOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
    }
}
