package dev.deva.devphone.line

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class HomeNetworkTest {
    private fun ip(s: String) = InetAddress.getByName(s)

    private val proxies = setOf("10.5.109.147", "10.5.109.131")

    @Test fun `resolving the ims domain to the configured p-cscf means home`() =
        assertTrue(HomeNetwork.isHome(listOf(ip("10.5.109.147")), proxies))

    @Test fun `the backup proxy also counts as home`() = assertTrue(HomeNetwork.isHome(listOf(ip("10.5.109.131")), proxies))

    @Test fun `no answer means away`() = assertFalse(HomeNetwork.isHome(emptyList(), proxies))

    @Test fun `a public answer means away`() = assertFalse(HomeNetwork.isHome(listOf(ip("49.44.1.10")), proxies))

    @Test fun `a private 10-dot that is not the proxy means away`() = assertFalse(HomeNetwork.isHome(listOf(ip("10.0.0.1")), proxies))

    @Test fun `other rfc1918 ranges mean away`() = assertFalse(HomeNetwork.isHome(listOf(ip("192.168.1.1"), ip("172.16.0.1")), proxies))

    @Test fun `ipv6 only means away`() = assertFalse(HomeNetwork.isHome(listOf(ip("2401:4900::1")), proxies))

    @Test fun `a mixed answer containing the proxy means home`() =
        assertTrue(HomeNetwork.isHome(listOf(ip("2401:4900::1"), ip("10.5.109.131")), proxies))

    @Test fun `no configured proxies means away`() = assertFalse(HomeNetwork.isHome(listOf(ip("10.5.109.147")), emptySet()))
}
