package dev.deva.devphone.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog.Calls
import android.util.Log

enum class HistoryType { Incoming, Outgoing, Missed, Rejected }

data class HistoryEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: HistoryType,
    val dateMillis: Long,
    val durationSec: Long,
)

class CallHistory(
    private val context: Context,
) {
    fun canRead() = granted(Manifest.permission.READ_CALL_LOG)

    fun record(
        number: String,
        name: String?,
        type: HistoryType,
        startMillis: Long,
        durationSec: Long,
    ) {
        if (!granted(Manifest.permission.WRITE_CALL_LOG)) return
        val values =
            ContentValues().apply {
                put(Calls.NUMBER, number)
                put(
                    Calls.TYPE,
                    when (type) {
                        HistoryType.Incoming -> Calls.INCOMING_TYPE
                        HistoryType.Outgoing -> Calls.OUTGOING_TYPE
                        HistoryType.Missed -> Calls.MISSED_TYPE
                        HistoryType.Rejected -> Calls.REJECTED_TYPE
                    },
                )
                put(Calls.DATE, startMillis)
                put(Calls.DURATION, durationSec)
                put(Calls.NEW, if (type == HistoryType.Missed) 1 else 0)
                put(Calls.IS_READ, if (type == HistoryType.Missed) 0 else 1)
                name?.let { put(Calls.CACHED_NAME, it) }
                put(Calls.PHONE_ACCOUNT_COMPONENT_NAME, ACCOUNT_COMPONENT)
                put(Calls.PHONE_ACCOUNT_ID, ACCOUNT_ID)
            }
        runCatching { context.contentResolver.insert(Calls.CONTENT_URI, values) }
            .onFailure { Log.w("DevPhone/History", "call log write failed", it) }
    }

    fun load(limit: Int = 200): List<HistoryEntry> {
        if (!canRead()) return emptyList()
        val out = ArrayList<HistoryEntry>()
        runCatching {
            context.contentResolver
                .query(
                    Calls.CONTENT_URI
                        .buildUpon()
                        .appendQueryParameter(Calls.LIMIT_PARAM_KEY, limit.toString())
                        .build(),
                    arrayOf(Calls._ID, Calls.NUMBER, Calls.CACHED_NAME, Calls.TYPE, Calls.DATE, Calls.DURATION),
                    "${Calls.PHONE_ACCOUNT_ID} = ?",
                    arrayOf(ACCOUNT_ID),
                    "${Calls.DATE} DESC",
                )?.use { c ->
                    while (c.moveToNext()) {
                        val type =
                            when (c.getInt(3)) {
                                Calls.OUTGOING_TYPE -> HistoryType.Outgoing
                                Calls.MISSED_TYPE -> HistoryType.Missed
                                Calls.REJECTED_TYPE, Calls.BLOCKED_TYPE -> HistoryType.Rejected
                                else -> HistoryType.Incoming
                            }
                        out += HistoryEntry(c.getLong(0), c.getString(1).orEmpty(), c.getString(2), type, c.getLong(4), c.getLong(5))
                    }
                }
        }.onFailure { Log.w("DevPhone/History", "call log read failed", it) }
        return out
    }

    private fun granted(p: String) = context.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val ACCOUNT_ID = "devphone-airtel-landline"
        const val ACCOUNT_COMPONENT = "dev.deva.devphone/.MainActivity"
    }
}
