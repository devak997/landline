Landline
========

An Android softphone for a single Airtel Fibernet IMS landline, used over
home Wi-Fi. Dialer, call history and contacts; nothing more. Kotlin, Jetpack
Compose (Material 3 Expressive), Linphone SDK for SIP and audio.

Not affiliated with Airtel or TP-Link. For use with your own line only.

How it works
------------

Airtel serves the line as a SIP account on its IMS, reachable only from the
home network. The app runs the SIP engine only when the Wi-Fi resolves the IMS
domain to a configured P-CSCF address, and stops everywhere else. No polling,
no timers, no keep-alives by default; the only periodic traffic is the hourly
re-registration. Calls use Android's telecom stack, so ringing, the lock-screen
call and audio routing are the system's own.

Requirements
------------

Android 14+ (arm64). An Airtel Fibernet line with an active landline and its
SIP credentials. The voice service must be reachable from the device's network
(router that routes Airtel's voice VLAN, or a third-party ONT setup).

Configuration
-------------

Enter your landline number and SIP password on first run. The IMS domain and
SIP servers default to the Andhra Pradesh circle (ap.ims.airtel.in,
10.5.109.147, 10.5.109.131) and are editable in Settings; use <circle>.ims.
airtel.in and your circle's P-CSCF for another region. Server addresses are
public Airtel infrastructure; the password is a credential, stored only in the
app's private storage and never committed here.

For reliable incoming calls behind a NAT router, forward SIP and RTP to the
phone, scoped to the proxy source (MikroTik example):

    /ip firewall nat add chain=dstnat in-interface=vlan660-voice src-address=<PROXY_IP> protocol=udp dst-port=5060 action=dst-nat to-addresses=<PHONE_IP> to-ports=5060
    /ip firewall nat add chain=dstnat in-interface=vlan660-voice src-address=<PROXY_IP> protocol=udp dst-port=30000-30019 action=dst-nat to-addresses=<PHONE_IP>

Without it, enable keep-alive packets in Settings.

Build
-----

Build with JDK 21 (detekt needs <=21, AGP needs 17+).

    ./gradlew check            # ktlint (Spotless), detekt, Android lint, tests
    ./gradlew :app:assembleRelease
    adb install -r app/build/outputs/apk/release/app-release.apk

Grant microphone, notifications, contacts and call-log permissions, and allow
unrestricted battery. Findings are fixed in source, not baselined; format with
./gradlew spotlessApply. Screenshots: ./gradlew recordRoborazziDebug.

Setting up the line
-------------------

See docs/airtel-fiber-sip.md for how Airtel's IMS landline works and how to
register it (educational only, not encouraged). Includes our worked example.

Security
--------

- Home is detected only when the IMS domain resolves to a configured P-CSCF,
  not any 10.x, so a rogue Wi-Fi can't trigger a registration. BSSID/gateway
  pinning (needs more permissions) would harden it further.
- Incoming caller ID (From/display-name) is untrusted; the number is matched
  against contacts, and INVITEs are filtered to the proxy source at the router.

License
-------

GPLv3 (see LICENSE), to match the Linphone SDK. Third-party components in
NOTICE. Provided without warranty.
