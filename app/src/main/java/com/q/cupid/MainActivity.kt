package com.q.cupid // Thay thế bằng package của bạn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.q.cupid.adapters.UserAdapter
import com.q.cupid.models.Chat
import com.q.cupid.models.User

class MainActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var rvChatList: RecyclerView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userAdapter: UserAdapter
    private var userDocRef: DocumentReference? = null
    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateOnlineStatus(true)
        }

        override fun onLost(network: Network) {
            updateOnlineStatus(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvUserName = findViewById(R.id.tvUserName)
        rvChatList = findViewById(R.id.rvChatList)

        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        userAdapter = UserAdapter { user ->
            getOrCreateChat(user)
        }
        rvChatList.adapter = userAdapter
        rvChatList.layoutManager = LinearLayoutManager(this)

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)

        // Lắng nghe danh sách người dùng online (loại trừ người dùng hiện tại)
        firestore.collection("users")
            .whereNotEqualTo("uid", auth.currentUser?.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MainActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                userAdapter.submitList(users)
            }

        // Lắng nghe trạng thái online/offline của người dùng hiện tại và tạo document nếu chưa có
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            userDocRef = firestore.collection("users").document(uid)

            userDocRef?.get()?.addOnSuccessListener { document ->
                if (!document.exists()) {
                    val newUser = User(uid, name = currentUser.displayName ?: "Unknown", email = currentUser.email, isOnline = true)
                    userDocRef?.set(newUser)
                } else {
                    updateOnlineStatus(true)
                }
            }?.addOnFailureListener { e ->
                Log.e("MainActivity", "Error getting or creating user document", e)
            }

            userDocRef?.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("MainActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    tvUserName.text = user?.name ?: "Unknown"
                } else {
                    tvUserName.text = "Unknown"
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updateOnlineStatus(true)
    }

    override fun onStop() {
        super.onStop()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun getOrCreateChat(partnerUser: User) {
        val currentUserId = auth.currentUser!!.uid
        val partnerUserId = partnerUser.uid

        firestore.collection("chats")
            .whereArrayContains("userIds", currentUserId)
            .whereArrayContains("userIds", partnerUserId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Cuộc trò chuyện đã tồn tại
                    val chat = documents.documents[0].toObject(Chat::class.java)!!
                    openChatActivity(chat.chatId, partnerUserId)
                } else {
                    // Tạo cuộc trò chuyện mới
                    val chatId = firestore.collection("chats").document().id
                    val newChat = Chat(chatId, listOf(currentUserId, partnerUserId))
                    firestore.collection("chats").document(chatId).set(newChat)
                        .addOnSuccessListener {
                            openChatActivity(chatId, partnerUserId)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("MainActivity", "Error creating chat", exception)
                            Toast.makeText(this, "Đã xảy ra lỗi khi tạo cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error getting chat", exception)
                Toast.makeText(this, "Đã xảy ra lỗi khi lấy thông tin cuộc trò chuyện", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChatActivity(chatId: String, partnerUid: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("CHAT_ID", chatId)
        intent.putExtra("partnerUid", partnerUid)
        startActivity(intent)
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        userDocRef?.update("isOnline", isOnline)
            ?.addOnSuccessListener {
                // Cập nhật trạng thái thành công
            }
            ?.addOnFailureListener { e ->
                // Xử lý lỗi cập nhật trạng thái
                Log.e("MainActivity", "Error updating online status", e)
            }
    }
}
