package com.r4bb1t.blockerspam.helper

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactsHelper {
    fun isNumberInContacts(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor -> cursor.moveToFirst() } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
