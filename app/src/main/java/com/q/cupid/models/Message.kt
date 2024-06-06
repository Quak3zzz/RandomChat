package com.q.cupid.models

data class Message(
    val messageId: String, // Thêm messageId
    val senderId: String,
    val text: String,
    val timestamp: Long
)
