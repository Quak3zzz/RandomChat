package com.q.cupid.models

data class Message(
    var messageId: String = "",
    var senderId: String = "",
    var text: String = "",
    var timestamp: Long = 0L
)
