package dev.deva.devphone.line

import java.net.InetAddress

object HomeNetwork {
    fun isHome(
        answers: List<InetAddress>,
        expectedProxies: Set<String>,
    ): Boolean {
        if (expectedProxies.isEmpty()) return false
        val expected = expectedProxies.mapNotNull { runCatching { InetAddress.getByName(it).hostAddress }.getOrNull() }.toSet()
        return answers.any { it.hostAddress in expected }
    }
}
