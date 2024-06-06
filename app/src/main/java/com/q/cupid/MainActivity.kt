package com.q.cupid // Thay thế bằng package của bạn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.q.cupid.adapters.ChatListAdapter
import com.q.cupid.models.Chat

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var tvUserName: TextView
    private lateinit var rvChatList: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var chatsListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore

        tvUserName = findViewById(R.id.tvUserName)
        rvChatList = findViewById(R.id.rvChatList) // Sửa id thành rvChatList

        // Hiển thị tên người dùng
        val currentUser = auth.currentUser
        tvUserName.text = currentUser?.displayName ?: "Unknown"

        // Khởi tạo adapter cho danh sách chat
        chatListAdapter = ChatListAdapter { chat ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CHAT_ID", chat.chatId)
            startActivity(intent)
        }
        rvChatList.adapter = chatListAdapter
        rvChatList.layoutManager = LinearLayoutManager(this)

        // Lắng nghe danh sách các cuộc trò chuyện của người dùng hiện tại
        chatsListener = firestore.collection("chats")
            .whereArrayContains("userIds", auth.currentUser!!.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MainActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val chats = snapshot?.toObjects(Chat::class.java) ?: emptyList()
                chatListAdapter.submitList(chats)
            }
    }

    override fun onStop() {
        super.onStop()
        chatsListener.remove() // Dừng lắng nghe khi Activity dừng
    }
}
