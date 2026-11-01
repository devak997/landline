package dev.deva.devphone.line

import android.content.Context
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.telecom.CallEndpointCompat
import dev.deva.devphone.BuildConfig
import dev.deva.devphone.data.CallHistory
import dev.deva.devphone.data.ContactInfo
import dev.deva.devphone.data.Contacts
import dev.deva.devphone.data.HistoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.Reason
import org.linphone.core.RegistrationState

class LineManager(
    private val app: Context,
    private val scope: CoroutineScope,
) {
    val configStore = LineConfigStore(app)
    val contacts = Contacts(app)
    val history = CallHistory(app)
    private val telecom = TelecomBridge(app, scope)
    private val factory: Factory =
        Factory.instance().apply {
            setDebugMode(BuildConfig.DEBUG, "DevPhone")
            enableLogcatLogs(BuildConfig.DEBUG)
        }

    private val _status = MutableStateFlow<LineStatus>(LineStatus.Away)
    val status: StateFlow<LineStatus> = _status.asStateFlow()

    private val _call = MutableStateFlow<ActiveCall?>(null)
    val call: StateFlow<ActiveCall?> = _call.asStateFlow()

    private val _historyVersion = MutableStateFlow(0)
    val historyVersion: StateFlow<Int> = _historyVersion.asStateFlow()

    private var core: Core? = null
    private var sipCall: Call? = null
    private var callerInfo: ContactInfo? = null
    private var atHome = false
    private var uiVisible = false
    private var onBackup = false

    init {
        telecom.listener =
            object : TelecomBridge.Listener {
                override fun onSystemAnswer() {
                    acceptSip()
                }

                override fun onSystemDisconnect() {
                    hangup()
                }

                override fun onEndpointChanged(type: Int) {
                    routeAudio(type)
                }
            }
        refreshState()
    }

    fun onHomeChanged(home: Boolean) {
        if (home == atHome) return
        atHome = home
        refreshState()
    }

    fun onConfigChanged() {
        stopCore()
        refreshState()
    }

    fun onUiVisible(visible: Boolean) {
        uiVisible = visible
        val c = core ?: return
        if (visible) {
            c.enterForeground()
        } else if (sipCall == null) {
            c.enterBackground()
        }
    }

    private fun refreshState() {
        val cfg = configStore.config.value
        when {
            !cfg.enabled || !cfg.isComplete -> {
                stopCore()
                _status.value = LineStatus.NeedsSetup
            }

            !atHome -> {
                stopCore()
                _status.value = LineStatus.Away
            }

            else -> {
                startCore(cfg)
            }
        }
    }

    private fun startCore(cfg: LineConfig) {
        if (core != null) return
        _status.value = LineStatus.Connecting
        onBackup = false
        val c =
            runCatching { factory.createCore(null, null, app) }.getOrElse {
                _status.value = LineStatus.Failed("Engine failed to start")
                return
            }
        c.isIpv6Enabled = false
        c.setUserAgent("DevPhone", BuildConfig.VERSION_NAME)
        c.setTransports(
            c.transports.apply {
                udpPort = SIP_PORT
                tcpPort = 0
                tlsPort = 0
                dtlsPort = 0
            },
        )
        c.setAudioPortRange(RTP_FIRST, RTP_LAST)
        c.isKeepAliveEnabled = cfg.keepAlive
        c.isPushNotificationEnabled = false
        c.useRfc2833ForDtmf = true
        c.isNativeRingingEnabled = true
        c.isVibrationOnIncomingCallEnabled = true
        c.isEchoCancellationEnabled = true
        c.incTimeout = 45

        c.config.setBool("audio", "android_disable_audio_focus_requests", true)
        c.audioPayloadTypes.forEach { it.enable(it.mimeType in CODECS) }
        c.addListener(listener)

        c.addAuthInfo(factory.createAuthInfo(cfg.number, cfg.authUser, cfg.password, null, null, cfg.domain))
        val params =
            c.createAccountParams().apply {
                setIdentityAddress(factory.createAddress(cfg.identity))
                applyProxy(cfg.proxy)
                expires = REGISTER_EXPIRES
                isRegisterEnabled = true
                pushNotificationAllowed = false
                remotePushNotificationAllowed = false
                setPublishEnabled(false)
                setUseInternationalPrefixForCallsAndChats(false)
            }
        val account = c.createAccount(params)
        c.addAccount(account)
        c.defaultAccount = account

        if (c.start() != 0) {
            _status.value = LineStatus.Failed("Engine failed to start")
            c.removeListener(listener)
            return
        }
        if (!uiVisible) c.enterBackground()
        core = c
    }

    private fun org.linphone.core.AccountParams.applyProxy(host: String) {
        val proxy = factory.createAddress("sip:$host:$SIP_PORT;transport=udp")
        setServerAddress(proxy)
        setRoutesAddresses(arrayOf(proxy))
    }

    private fun stopCore() {
        val c = core ?: return
        core = null
        sipCall?.let { runCatching { it.terminate() } }
        c.removeListener(listener)
        runCatching { c.stop() }
        if (_call.value != null) finishCall("Line went offline")
    }

    fun dial(raw: String): String? {
        val c = core ?: return "Landline is not connected"
        if (_status.value != LineStatus.Ready) return "Landline is not ready yet"
        if (sipCall != null) return "Already in a call"
        val number = PhoneNumbers.toDialString(raw) ?: return "That number can't be dialled"
        val cfg = configStore.config.value
        val address = factory.createAddress("sip:$number@${cfg.domain}") ?: return "That number can't be dialled"
        address.setUriParam("user", "phone")
        val params =
            c.createCallParams(null)?.apply {
                mediaEncryption = MediaEncryption.None
                setVideoEnabled(false)
            } ?: return "Couldn't start the call"
        callerInfo = contacts.lookup(number)
        _call.value = ActiveCall(number, callerInfo?.name, Direction.Outgoing, CallPhase.Dialing, photo = callerInfo?.photo)
        if (c.inviteAddressWithParams(address, params) == null) {
            _call.value = null
            return "Couldn't start the call"
        }
        return null
    }

    fun answer() {
        acceptSip()
        telecom.answered()
    }

    private fun acceptSip() {
        val call = sipCall ?: return
        if (call.state == Call.State.IncomingReceived || call.state == Call.State.IncomingEarlyMedia) {
            call.accept()
            _call.value = _call.value?.copy(phase = CallPhase.Connecting)
        }
    }

    fun hangup() {
        val call = sipCall ?: return
        val ringingIn =
            call.dir == Call.Dir.Incoming &&
                (call.state == Call.State.IncomingReceived || call.state == Call.State.IncomingEarlyMedia)
        if (ringingIn) call.decline(Reason.Declined) else call.terminate()
    }

    fun toggleMute() {
        val call = sipCall ?: return
        call.microphoneMuted = !call.microphoneMuted
        _call.value = _call.value?.copy(muted = call.microphoneMuted)
    }

    fun toggleSpeaker() {
        val on = !(_call.value?.speaker ?: false)
        if (!telecom.setSpeaker(on)) {
            routeAudio(if (on) CallEndpointCompat.TYPE_SPEAKER else CallEndpointCompat.TYPE_EARPIECE)
        }
    }

    fun sendDtmf(digit: Char) {
        sipCall?.sendDtmf(digit)
    }

    private fun routeAudio(endpointType: Int) {
        val c = core ?: return
        val call = sipCall ?: return
        val wanted =
            when (endpointType) {
                CallEndpointCompat.TYPE_SPEAKER -> {
                    listOf(AudioDevice.Type.Speaker)
                }

                CallEndpointCompat.TYPE_BLUETOOTH -> {
                    listOf(AudioDevice.Type.Bluetooth, AudioDevice.Type.HearingAid)
                }

                CallEndpointCompat.TYPE_WIRED_HEADSET -> {
                    listOf(
                        AudioDevice.Type.Headset,
                        AudioDevice.Type.Headphones,
                        AudioDevice.Type.GenericUsb,
                    )
                }

                else -> {
                    listOf(AudioDevice.Type.Earpiece)
                }
            }
        val devices = c.audioDevices
        devices
            .firstOrNull { it.type in wanted && it.hasCapability(AudioDevice.Capabilities.CapabilityPlay) }
            ?.let { call.outputAudioDevice = it }
        val mic =
            devices.firstOrNull { it.type in wanted && it.hasCapability(AudioDevice.Capabilities.CapabilityRecord) }
                ?: devices.firstOrNull { it.type == AudioDevice.Type.Microphone }
        mic?.let { call.inputAudioDevice = it }
        _call.value = _call.value?.copy(speaker = endpointType == CallEndpointCompat.TYPE_SPEAKER)
    }

    private val listener =
        object : CoreListenerStub() {
            override fun onAccountRegistrationStateChanged(
                core: Core,
                account: Account,
                state: RegistrationState?,
                message: String,
            ) {
                when (state) {
                    RegistrationState.Ok -> _status.value = LineStatus.Ready
                    RegistrationState.Progress -> if (_status.value != LineStatus.Ready) _status.value = LineStatus.Connecting
                    RegistrationState.Failed -> onRegistrationFailed(account)
                    else -> Unit
                }
            }

            override fun onCallStateChanged(
                core: Core,
                call: Call,
                state: Call.State?,
                message: String,
            ) {
                onCallState(call, state ?: return)
            }

            override fun onAudioDevicesListUpdated(core: Core) {
                if (_call.value?.speaker == true) routeAudio(CallEndpointCompat.TYPE_SPEAKER)
            }
        }

    private fun onRegistrationFailed(account: Account) {
        val cfg = configStore.config.value
        val reason = account.error
        if (reason == Reason.Unauthorized || reason == Reason.Forbidden) {
            _status.value = LineStatus.Failed("Password rejected")
            return
        }
        if (!onBackup && cfg.backupProxy.isNotBlank()) {
            onBackup = true
            account.params
                .clone()
                .apply { applyProxy(cfg.backupProxy) }
                .let { account.params = it }
            account.refreshRegister()
            return
        }
        _status.value = LineStatus.Failed("Airtel isn't answering")
    }

    private fun onCallState(
        call: Call,
        state: Call.State,
    ) {
        val incoming = call.dir == Call.Dir.Incoming
        if (state == Call.State.IncomingReceived) {
            if (sipCall != null && sipCall != call) {
                call.decline(Reason.Busy)
                return
            }

            val number = PhoneNumbers.fromUri(call.remoteAddress.asStringUriOnly())
            callerInfo = contacts.lookup(number)
            _call.value =
                ActiveCall(
                    number,
                    callerInfo?.name ?: call.remoteAddress.displayName?.takeIf { it.isNotBlank() },
                    Direction.Incoming,
                    CallPhase.Ringing,
                    photo = callerInfo?.photo,
                )
            core?.enterForeground()
        }
        if (state == Call.State.OutgoingInit || state == Call.State.IncomingReceived) {
            sipCall = call
            val ac = _call.value ?: return
            telecom.begin(ac.name ?: PhoneNumbers.pretty(ac.number), ac.number, incoming)
        }
        if (call != sipCall) return

        val phase = CallPhases.fromLinphone(state.name, incoming) ?: return
        val current = _call.value ?: return
        when (phase) {
            CallPhase.Active -> {
                if (current.connectedAt == null) {
                    _call.value = current.copy(phase = CallPhase.Active, connectedAt = System.currentTimeMillis())
                    telecom.active()
                }
            }

            CallPhase.Ended -> {
                if (state == Call.State.Released) {
                    onReleased(call)
                } else {
                    _call.value = current.copy(phase = CallPhase.Ended, endReason = CallPhases.reasonText(call.reason.name))
                }
            }

            else -> {
                if (current.phase != phase) _call.value = current.copy(phase = phase)
            }
        }
    }

    private fun onReleased(call: Call) {
        val log = call.callLog
        val current = _call.value
        val type =
            when {
                call.dir == Call.Dir.Outgoing -> HistoryType.Outgoing
                log.status == Call.Status.Missed -> HistoryType.Missed
                log.status == Call.Status.Declined -> HistoryType.Rejected
                log.status == Call.Status.Success -> HistoryType.Incoming
                else -> HistoryType.Missed
            }
        current?.let {
            history.record(it.number, it.name, type, log.startDate * 1000, log.duration.toLong())
            _historyVersion.value++
        }
        telecom.end(
            when (type) {
                HistoryType.Missed -> DisconnectCause.MISSED
                HistoryType.Rejected -> DisconnectCause.REJECTED
                else -> if (call.reason == Reason.None) DisconnectCause.LOCAL else DisconnectCause.REMOTE
            },
        )
        sipCall = null
        finishCall(CallPhases.reasonText(call.reason.name))
        if (!uiVisible) core?.enterBackground()
    }

    private fun finishCall(reason: String?) {
        val ended = _call.value?.copy(phase = CallPhase.Ended, endReason = reason) ?: return
        _call.value = ended
        scope.launch {
            delay(1_500)
            if (_call.value === ended) _call.value = null
        }
    }

    val engineVersion: String? get() = core?.version

    companion object {
        const val SIP_PORT = 5060
        const val RTP_FIRST = 30000
        const val RTP_LAST = 30019
        const val REGISTER_EXPIRES = 3600
        val CODECS = setOf("PCMA", "PCMU", "G722")
    }
}
