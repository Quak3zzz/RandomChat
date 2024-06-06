package com.q.cupid.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.q.cupid.ChatActivity
import com.q.cupid.R
import com.q.cupid.models.Chat
import com.q.cupid.models.Message
import com.q.cupid.models.User
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(private val onChatClick: (Chat) -> Unit) :
    ListAdapter<User, ChatListAdapter.ChatViewHolder>(UserDiffCallback) {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPartnerName: TextView = itemView.findViewById(R.id.tvPartnerName)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val ivPartnerAvatar: ImageView = itemView.findViewById(R.id.ivPartnerAvatar)

        fun bind(user: User) {
            tvPartnerName.text = user.name
            user.profileImage?.let {
                Glide.with(itemView).load(it).into(ivPartnerAvatar)
            }

            getChatWithPartnerId(user.uid)
                .addOnSuccessListener { result ->
                    result.documents.firstOrNull()?.let { documentSnapshot ->
                        val chat = documentSnapshot.toObject(Chat::class.java)!!
                        itemView.setOnClickListener { onChatClick(chat) }

                        chat.chatId?.let { chatId ->
                            Firebase.firestore.collection("chats").document(chatId).collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (!querySnapshot.isEmpty) {
                                        val lastMessage = querySnapshot.documents[0].toObject(Message::class.java)
                                        tvLastMessage.text = lastMessage?.text ?: ""
                                        tvTimestamp.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMessage?.timestamp ?: 0))
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("ChatListAdapter", "Error getting last message", exception)
                                }
                        }
                    } ?: run {
                        // Nếu chưa có cuộc trò chuyện, khi click vào sẽ tạo mới
                        itemView.setOnClickListener {
                            createNewChatWithUser(user)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ChatListAdapter", "Error getting chat with partner", exception)
                }
        }

        private fun createNewChatWithUser(partnerUser: User) {
            val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
            val chatId = Firebase.firestore.collection("chats").document().id
            val combinedUserIds = getCombinedUserIds(currentUserId, partnerUser.uid) // Tính combinedUserIds

            val newChat = Chat(chatId, listOf(currentUserId, partnerUser.uid))
            Firebase.firestore.collection("chats").document(chatId).set(newChat)
                .addOnSuccessListener {
                    // Cập nhật combinedUserIds sau khi tạo document chat thành công
                    Firebase.firestore.collection("chats").document(chatId).update("combinedUserIds", combinedUserIds)

                    onChatClick(newChat)
                }
                .addOnFailureListener { exception ->
                    Log.e("ChatListAdapter", "Error creating chat", exception)
                    Toast.makeText(itemView.context, "Đã xảy ra lỗi khi tạo cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getChatWithPartnerId(partnerId: String) = Firebase.firestore.collection("chats")
        .whereEqualTo("combinedUserIds", getCombinedUserIds(FirebaseAuth.getInstance().currentUser!!.uid, partnerId))
        .limit(1)
        .get()

    private fun getCombinedUserIds(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "$userId1 _$userId2" else "$userId2 _$userId1" // Sửa thành _
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    companion object {
        val UserDiffCallback = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.uid == newItem.uid
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }
}
