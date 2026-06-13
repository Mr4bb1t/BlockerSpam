package com.r4bb1t.blockerspam.data

data class BlockedMessageSummary(
    val sender: String,
    val messageCount: Int,
    val lastMessageTime: Long,
    val lastMatchedKeyword: String
)
