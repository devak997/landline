Airtel Fibernet landline over SIP
=================================

For educational purposes only. This is unofficial and not encouraged: it may
breach your Airtel terms of service, and it only works with your own line and
credentials. Use at your own risk.

How it works
------------

Airtel Xstream Fiber delivers the landline as a SIP account on its IMS, carried
on a separate voice VLAN (usually 660) and reachable only from the home network,
not the public internet. With the Airtel ONT, or a third-party ONT in
transparent mode plus a router that routes that VLAN, the line can register on a
softphone (or the Landline app in this repo). Distilled from our setup and the
India Broadband Forum thread "Are VoIP Services or SIP Settings available in
Airtel Xstream Fiber Broadband".

SIP details
-----------

Values vary by circle:

- Domain:   <circle>.ims.airtel.in   (ap, ka, rj, wb, hp, ...)
- Proxy:    two P-CSCF IPs in 10.x
- VLAN:     660 (run a VLAN scan if unsure; a few areas differ)
- Username: +91<number>
- Auth ID:  +91<number>@<circle>.ims.airtel.in
- Password: Airtel's old default is "Nokia@1", set regardless of ONT brand.
            Airtel has begun changing it per line, so read yours from the ONT.

Read them from the Airtel ONT: its VoIP/WAN pages, or Maintenance > Logs search
for "SIP", or an exported config. The voice WAN carries a static IP, mask and
gateway in the 100.x range, plus host routes to the two P-CSCF IPs.

Network (MikroTik)
------------------

ONT in transparent mode, VLAN 660 tagged toward the router:

    /interface vlan add name=vlan660-voice interface=ether1 vlan-id=660
    /ip address add address=<VOICE_IP>/<MASK> interface=vlan660-voice
    /ip route add dst-address=10.0.0.0/8 gateway=<VOICE_GW>
    /ip firewall nat add chain=srcnat out-interface=vlan660-voice action=masquerade
    /ip dns static add name=<circle>.ims.airtel.in address=<PROXY_IP>

Use the voice IP, mask and gateway the ONT was provisioned with. If internet
works and the route is present, registration usually works.

Softphone
---------

- Domain / registrar: <circle>.ims.airtel.in (or the proxy IP directly)
- Username: +91<number>   Auth ID: +91<number>@<circle>.ims.airtel.in
- Password: from the ONT
- Transport: UDP, port 5060
- Outbound proxy: the primary P-CSCF IP
- On a PC, add a hosts entry mapping the domain to the proxy IP.

Dial outgoing numbers with a leading 0 (0XXXXXXXXXX). Airtel's IMS allows only
one registered device at a time.

Our setup (worked example, educational only)
--------------------------------------------

- Circle: Andhra Pradesh. ISP ONT replaced with a TP-Link XZ000-G7 in
  transparent mode; router a MikroTik hEX S.
- Domain: <circle>.ims.airtel.in. P-CSCF: <PCSCF_1> and <PCSCF_2>, UDP 5060.
  Both P-CSCF addresses are 10.x and are shared circle-wide; discover yours
  by resolving the IMS domain on the voice VLAN.
- Voice VLAN 660, with a provisioned static voice IP and gateway handed out
  by your OLT (a 100.64.0.0/10 carrier-NAT address). These are line-specific,
  so read your own from the ONT rather than copying any value.
- On the hEX S:

      /interface vlan add name=vlan660-voice interface=ether1 vlan-id=660
      /ip address add address=<VOICE_IP>/<prefix> interface=vlan660-voice
      /ip route add dst-address=10.0.0.0/8 gateway=<VOICE_GW>
      /ip firewall nat add chain=srcnat out-interface=vlan660-voice action=masquerade
      /ip dns static add name=<circle>.ims.airtel.in address=<PCSCF_1>

  From the LAN, both P-CSCF IPs ping, and a SIP OPTIONS probe returns the IMS
  "407 Proxy Authentication Required", confirming the path. The voice IP,
  gateway, number and password are all line-specific and are not published
  here.

Troubleshooting
---------------

403 Forbidden after the 401 challenge
  Wrong or changed password. "Nokia@1" is the old default; newer or updated
  lines have a per-line password. Read the current one from the ONT while it is
  online and synced. A digest that reaches the server and is refused means the
  password is wrong, not the client.

Cannot reach the SIP servers
  The P-CSCF IPs (10.x) live only on the voice VLAN. Add the static route to the
  10.x subnet via the voice gateway.

Registers but outgoing calls fail
  Prefix a 0 to the dialled number. Turn off SIP ALG on any router in the path.

Incoming calls do not ring, or drop in 1-2 seconds
  Behind NAT, forward UDP 5060 and the RTP range to the client, scoped to the
  proxy source. Do not register the same line on two devices.

Call drops every few minutes
  The ONT keeps re-registering the same account (registration collision). Use
  the softphone only, and stop the ONT from registering the line.

Locked ISP ONT
  Some ONTs hide the config and credentials. A third-party ONT in transparent
  mode with your own router avoids that, but you still need the SIP password.
