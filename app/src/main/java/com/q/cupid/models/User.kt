package com.q.cupid.models // Thay thế bằng package của bạn

data class User(
    val uid: String? = null,
    val name: String = "", // Không cho phép name là null
    val email: String? = null,
    val profileImage: String = "",
    var isOnline: Boolean = false

)
