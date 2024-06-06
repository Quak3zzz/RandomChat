package com.q.cupid.models

data class Chat(
    val chatId: String = "",
    val userIds: List<String> = emptyList(),
    var combinedUserIds: String = "",
    val timestamp: Long = System.currentTimeMillis() // Thêm trường timestamp
)
