package dev.deva.devphone.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup
import dev.deva.devphone.line.PhoneNumbers

data class ContactInfo(
    val name: String,
    val photo: Uri?,
)

class Contacts(
    private val context: Context,
) {
    private val cache = HashMap<String, ContactInfo?>()

    fun canRead() = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun lookup(number: String): ContactInfo? {
        if (number.isBlank() || !canRead()) return null
        val key = PhoneNumbers.matchKey(number).ifEmpty { number }
        if (cache.containsKey(key)) return cache[key]
        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val info =
            runCatching {
                context.contentResolver
                    .query(
                        uri,
                        arrayOf(PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_THUMBNAIL_URI),
                        null,
                        null,
                        null,
                    )?.use { c ->
                        if (c.moveToFirst()) ContactInfo(c.getString(0), c.getString(1)?.let(Uri::parse)) else null
                    }
            }.getOrNull()
        cache[key] = info
        return info
    }

    fun invalidate() = cache.clear()
}
