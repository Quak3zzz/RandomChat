package com.q.cupid // Thay thế bằng package của bạn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.q.cupid.models.User

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etRegisterEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etRegisterPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etRegisterName = findViewById<EditText>(R.id.etRegisterName)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val email = etRegisterEmail.text.toString()
            val password = etRegisterPassword.text.toString()
            val name = etRegisterName.text.toString().ifEmpty { "Unknown" }

            registerUser(email, password, name) // Gọi hàm registerUser đã được tách ra
        }
    }

    // Hàm registerUser được đưa ra ngoài onCreate
    private fun registerUser(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    val uid = user?.uid
                    if (uid != null) {
                        val db = Firebase.firestore
                        val newUser = User(uid, name, email, isOnline = false)

                        // Kiểm tra xem document đã tồn tại chưa (sử dụng get().addOnSuccessListener)
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // Document đã tồn tại, thông báo lỗi hoặc yêu cầu đăng nhập
                                    Toast.makeText(baseContext, "User already exists. Please log in.", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Document chưa tồn tại, tạo mới
                                    db.collection("users").document(uid).set(newUser)
                                        .addOnSuccessListener {
                                            // Đăng ký thành công, chuyển đến LoginActivity
                                            startActivity(Intent(this, LoginActivity::class.java))
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            // Xử lý lỗi đăng ký chi tiết hơn
                                            Toast.makeText(baseContext, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            Log.e("RegisterActivity", "Error registering user", e)
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                // Xử lý lỗi khi kiểm tra document
                                Toast.makeText(baseContext, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("RegisterActivity", "Error checking user document", e)
                            }
                    }
                } else {
                    // Đăng ký thất bại, hiển thị thông báo lỗi chi tiết hơn
                    Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
