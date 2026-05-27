package com.r4bb1t.blockerspam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist")
data class WhitelistEntry(
    @PrimaryKey
    val number: String,
    val addedAt: Long = System.currentTimeMillis(),
    val label: String = ""
)
