package dev.deva.devphone.line

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeWatcher(
    context: Context,
    private val scope: CoroutineScope,
    private val imsDomain: () -> String,
    private val expectedProxies: () -> Set<String>,
    private val onHome: (Boolean) -> Unit,
) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val verdicts = HashMap<Network, Boolean>()
    private val checks = HashMap<Network, Job>()
    private var published: Boolean? = null

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = check(network)

            override fun onLinkPropertiesChanged(
                network: Network,
                lp: LinkProperties,
            ) {
                if (verdicts[network] != true) check(network)
            }

            override fun onLost(network: Network) {
                scope.launch {
                    checks.remove(network)?.cancel()
                    verdicts.remove(network)
                    publish()
                }
            }
        }

    fun start() {
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        cm.registerNetworkCallback(request, callback)
    }

    fun stop() = runCatching { cm.unregisterNetworkCallback(callback) }

    private fun check(network: Network) {
        scope.launch {
            checks.remove(network)?.cancel()
            checks[network] =
                scope.launch {
                    for (wait in RETRY_DELAYS_MS) {
                        if (wait > 0) delay(wait)
                        val home =
                            withContext(Dispatchers.IO) {
                                val answers = runCatching { network.getAllByName(imsDomain()).toList() }.getOrDefault(emptyList())
                                HomeNetwork.isHome(answers, expectedProxies())
                            }
                        verdicts[network] = home
                        publish()
                        if (home) break
                    }
                }
        }
    }

    private fun publish() {
        val home = verdicts.values.any { it }
        if (home != published) {
            published = home
            onHome(home)
        }
    }

    private companion object {
        val RETRY_DELAYS_MS = longArrayOf(0, 3_000, 10_000)
    }
}
