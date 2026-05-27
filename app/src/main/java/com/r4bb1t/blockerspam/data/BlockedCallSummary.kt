package com.r4bb1t.blockerspam.data

data class BlockedCallSummary(
    val number: String,
    val callCount: Int,
    val lastCallTime: Long
)
