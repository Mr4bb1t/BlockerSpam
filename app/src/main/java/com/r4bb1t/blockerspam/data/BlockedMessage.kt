package com.r4bb1t.blockerspam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_messages")
data class BlockedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val matchedKeyword: String,
    val timestamp: Long = System.currentTimeMillis()
)
