package com.q.cupid // Thay thế bằng package của bạn

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.q.cupid.adapters.MessageAdapter
import com.q.cupid.models.Message
import com.q.cupid.models.User
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    // Khai báo các thành phần giao diện
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSendMessage: Button
    private lateinit var tvPartnerName: TextView
    private lateinit var tvPartnerStatus: TextView

    // Khai báo các biến khác
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var chatId: String
    private lateinit var partnerUid: String
    private lateinit var partnerUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Khởi tạo các thành phần giao diện
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        tvPartnerName = findViewById(R.id.tvPartnerName)
        tvPartnerStatus = findViewById(R.id.tvPartnerStatus)

        // Khởi tạo các biến khác
        firestore = Firebase.firestore
        auth = FirebaseAuth.getInstance()
        chatId = intent.getStringExtra("chatId") ?: ""
        partnerUid = intent.getStringExtra("partnerUid") ?: ""

        messageAdapter = MessageAdapter(auth.currentUser!!.uid)
        rvMessages.adapter = messageAdapter
        rvMessages.layoutManager = LinearLayoutManager(this)

        // Lắng nghe tin nhắn mới
        firestore.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("ChatActivity", "Error listening for messages", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    messageAdapter.submitList(messages)
                    rvMessages.scrollToPosition(messages.size - 1) // Cuộn đến tin nhắn mới nhất
                }
            }

        // Lấy thông tin người dùng đang chat cùng và lắng nghe trạng thái online/offline
        firestore.collection("users").document(partnerUid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatActivity", "Listen user failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    partnerUser = snapshot.toObject(User::class.java)!!
                    tvPartnerName.text = partnerUser.name
                    tvPartnerStatus.text = if (partnerUser.isOnline) "Online" else "Offline"
                }
            }

        btnSendMessage.setOnClickListener {
            val messageText = etMessage.text.toString()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun sendMessage(messageText: String) {
        val message = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = auth.currentUser!!.uid,
            text = messageText,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("chats").document(chatId).collection("messages").add(message)
            .addOnSuccessListener {
                etMessage.text.clear()
            }
            .addOnFailureListener { exception ->
                Log.e("ChatActivity", "Error sending message", exception)
                Toast.makeText(this, "Đã xảy ra lỗi khi gửi tin nhắn", Toast.LENGTH_SHORT).show()
            }
    }
}
