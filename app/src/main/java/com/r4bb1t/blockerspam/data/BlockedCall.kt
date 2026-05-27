package com.r4bb1t.blockerspam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_calls")
data class BlockedCall(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val timestamp: Long = System.currentTimeMillis()
)
