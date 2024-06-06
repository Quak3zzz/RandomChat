package com.q.cupid // Thay thế bằng package của bạn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.q.cupid.models.User

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val uid = user?.uid
                        if (uid != null) {
                            updateOnlineStatus(uid, true) // Cập nhật trạng thái online

                            // Đăng nhập thành công, chuyển đến màn hình chính
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        // Đăng nhập thất bại, hiển thị thông báo lỗi
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun updateOnlineStatus(uid: String, isOnline: Boolean) {
        val db = Firebase.firestore
        val userDocRef = db.collection("users").document(uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Document tồn tại, cập nhật trạng thái
                    userDocRef.update("isOnline", isOnline)
                } else {
                    // Document không tồn tại, tạo mới với trạng thái online
                    val user = auth.currentUser // Lấy thông tin user từ Firebase Authentication
                    val newUser = User(uid, name = user?.displayName ?: "Unknown", email = user?.email, isOnline = true)
                    userDocRef.set(newUser)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error updating online status", e)
                Toast.makeText(baseContext, "Error updating online status", Toast.LENGTH_SHORT).show()
            }
    }
}
