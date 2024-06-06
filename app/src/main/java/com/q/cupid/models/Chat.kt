package com.q.cupid.models // Thay thế bằng package của bạn

data class Chat(
    val chatId: String = "", // ID của cuộc trò chuyện
    val userIds: List<String> = emptyList() // Danh sách UID của những người dùng tham gia cuộc trò chuyện
    // ... các thuộc tính khác (ví dụ: lastMessage, timestamp, ...)
)
