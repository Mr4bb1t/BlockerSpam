package com.r4bb1t.blockerspam.helper

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactsHelper {
    private fun generateNumberVariations(number: String): Set<String> {
        val variations = mutableSetOf<String>()
        val clean = number.replace(Regex("[^+\\d]"), "")
        if (clean.isBlank()) return variations
        
        variations.add(number)
        variations.add(clean)

        var local = clean
        if (local.startsWith("+55")) {
            local = local.removePrefix("+55")
        } else if (local.startsWith("0")) {
            local = local.removePrefix("0")
            if (local.length >= 12) {
                local = local.drop(2) // Remove operadora
            }
        } else if (local.startsWith("55") && local.length > 11) {
            local = local.removePrefix("55")
        }
        
        if (local.length >= 10) {
            variations.add(local)
            variations.add("+55$local")
            variations.add("0$local")
            
            // Lida com o 9º dígito em celulares (DDD + 9/8 dígitos = 11 ou 10)
            if (local.length == 11 && local[2] == '9') {
                val without9 = local.substring(0, 2) + local.substring(3)
                variations.add(without9)
                variations.add("+55$without9")
                variations.add("0$without9")
            } else if (local.length == 10) {
                val with9 = local.substring(0, 2) + "9" + local.substring(2)
                variations.add(with9)
                variations.add("+55$with9")
                variations.add("0$with9")
            }
        }
        return variations
    }

    fun isNumberInContacts(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        
        val variations = generateNumberVariations(number)
        
        for (v in variations) {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(v)
            )
            try {
                val exists = context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor -> cursor.moveToFirst() } ?: false
                
                if (exists) return true
            } catch (e: Exception) {
                // Ignore and try next variation
            }
        }
        return false
    }
}
