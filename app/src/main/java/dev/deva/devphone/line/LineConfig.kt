package dev.deva.devphone.line

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LineConfig(
    val number: String = "",
    val domain: String = "ap.ims.airtel.in",
    val proxy: String = "10.5.109.147",
    val backupProxy: String = "10.5.109.131",
    val password: String = "",
    val enabled: Boolean = true,
    val keepAlive: Boolean = false,
) {
    val identity: String get() = "sip:$number@$domain"

    val authUser: String get() = "$number@$domain"
    val isComplete: Boolean
        get() = number.isNotBlank() && domain.isNotBlank() && proxy.isNotBlank() && password.isNotBlank()
}

class LineConfigStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("line", Context.MODE_PRIVATE)
    private val _config = MutableStateFlow(read())
    val config: StateFlow<LineConfig> = _config.asStateFlow()

    private fun read(): LineConfig {
        val d = LineConfig()
        return LineConfig(
            number = prefs.getString("number", d.number)!!,
            domain = prefs.getString("domain", d.domain)!!,
            proxy = prefs.getString("proxy", d.proxy)!!,
            backupProxy = prefs.getString("backupProxy", d.backupProxy)!!,
            password = prefs.getString("password", "")!!,
            enabled = prefs.getBoolean("enabled", true),
            keepAlive = prefs.getBoolean("keepAlive", false),
        )
    }

    fun save(c: LineConfig) {
        prefs.edit {
            putString("number", c.number.trim())
            putString("domain", c.domain.trim())
            putString("proxy", c.proxy.trim())
            putString("backupProxy", c.backupProxy.trim())
            putString("password", c.password)
            putBoolean("enabled", c.enabled)
            putBoolean("keepAlive", c.keepAlive)
        }
        _config.value = read()
    }
}
