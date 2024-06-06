package com.q.cupid // Thay thế bằng package của bạn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.widget.Button
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
import com.q.cupid.models.User
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // Khai báo các thành phần giao diện
    private lateinit var tvUserName: TextView
    private lateinit var rvChatList: RecyclerView
    private lateinit var btnStartRandomChat: Button

    // Khai báo các biến khác
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

        // Khởi tạo các thành phần giao diện
        tvUserName = findViewById(R.id.tvUserName)
        rvChatList = findViewById(R.id.rvChatList) // Đảm bảo ID của RecyclerView là rvMessages
        btnStartRandomChat = findViewById(R.id.btnStartRandomChat)

        // Khởi tạo các biến khác
        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Khởi tạo adapter và gán cho RecyclerView
        userAdapter = UserAdapter { user: User ->
            user.uid?.let { partnerUid ->
                createChatRoom(partnerUid)
            } ?: run {
                Toast.makeText(this, "Lỗi: Không tìm thấy UID người dùng", Toast.LENGTH_SHORT).show()
            }
        }
        rvChatList.adapter = userAdapter
        rvChatList.layoutManager = LinearLayoutManager(this)

        // Đăng ký NetworkCallback để lắng nghe thay đổi kết nối mạng
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)

        // Lắng nghe danh sách người dùng online (loại trừ người dùng hiện tại)
        firestore.collection("users")
            .whereEqualTo("isOnline", true)
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
                    // Document không tồn tại, tạo mới
                    val newUser = User(uid, name = currentUser.displayName ?: "Unknown", email = currentUser.email, isOnline = true)
                    userDocRef?.set(newUser)
                } else {
                    updateOnlineStatus(true) // Cập nhật trạng thái online khi người dùng đăng nhập
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

        btnStartRandomChat.setOnClickListener {
            findRandomUser()
        }
    }

    override fun onStart() {
        super.onStart()
        updateOnlineStatus(true) // Cập nhật trạng thái online khi ứng dụng được mở lại
    }

    override fun onStop() {
        super.onStop()
        connectivityManager.unregisterNetworkCallback(networkCallback) // Hủy đăng ký listener
    }

    private fun findRandomUser() {
        firestore.collection("users")
            .whereEqualTo("isOnline", true)
            .whereNotEqualTo("uid", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Không tìm thấy người dùng phù hợp", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val randomIndex = Random.nextInt(documents.size())
                val partner = documents.documents[randomIndex]
                val partnerUid = partner.id
                createChatRoom(partnerUid)
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error finding random user", exception)
                Toast.makeText(this, "Đã xảy ra lỗi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createChatRoom(partnerUid: String) {
        val chatId = firestore.collection("chats").document().id
        val chatData = hashMapOf(
            "chatId" to chatId,
            "user1" to auth.currentUser!!.uid,
            "user2" to partnerUid
        )

        firestore.collection("chats").document(chatId).set(chatData)
            .addOnSuccessListener {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                intent.putExtra("partnerUid", partnerUid)
                startActivity(intent)
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error creating chat room", exception)
                Toast.makeText(this, "Đã xảy ra lỗi", Toast.LENGTH_SHORT).show()
            }
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
