package com.q.cupid // Thay thế bằng package của bạn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.q.cupid.models.User

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var ivProfileImage: ImageView
    private lateinit var btnChooseImage: Button
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etRegisterEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etRegisterPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etRegisterName = findViewById<EditText>(R.id.etRegisterName)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnChooseImage = findViewById(R.id.btnChooseImage)

        btnChooseImage.setOnClickListener {
            openImageChooser()
        }

        btnRegister.setOnClickListener {
            val email = etRegisterEmail.text.toString()
            val password = etRegisterPassword.text.toString()
            val name = etRegisterName.text.toString().ifEmpty { "Unknown" }

            registerUser(email, password, name)
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            Glide.with(this).load(selectedImageUri).into(ivProfileImage)
        }
    }

    private fun registerUser(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    val uid = user?.uid
                    if (uid != null && selectedImageUri != null) {
                        uploadImageToFirebaseStorage(uid, selectedImageUri!!, name, email)
                    } else if (uid != null) {
                        saveUserToFirestore(uid, name, email, "")
                    } else {
                        // Xử lý khi không lấy được uid
                        Toast.makeText(baseContext, "Registration failed: UID not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Đăng ký thất bại, hiển thị thông báo lỗi
                    Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadImageToFirebaseStorage(uid: String, imageUri: Uri, name: String, email: String) {
        val storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("images/${uid}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Lấy URL của ảnh sau khi tải lên
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val profileImageUrl = uri.toString()
                    saveUserToFirestore(uid, name, email, profileImageUrl)
                }
            }
            .addOnFailureListener { e ->
                // Xử lý lỗi tải ảnh lên
                Toast.makeText(baseContext, "Image upload failed.", Toast.LENGTH_SHORT).show()
                Log.e("RegisterActivity", "Error uploading image", e)
            }
    }

    private fun saveUserToFirestore(uid: String, name: String, email: String, profileImageUrl: String) {
        val db = Firebase.firestore
        val newUser = User(uid, name, email, profileImageUrl, isOnline = false)
        db.collection("users").document(uid).set(newUser)
            .addOnSuccessListener {
                // Đăng ký thành công, chuyển đến LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                // Xử lý lỗi đăng ký
                Toast.makeText(baseContext, "Registration failed.", Toast.LENGTH_SHORT).show()
                Log.e("RegisterActivity", "Error registering user", e) // Ghi log lỗi
            }
    }
}
