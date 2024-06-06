package com.q.cupid.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.q.cupid.R
import com.q.cupid.models.Chat
import com.q.cupid.models.Message
import com.q.cupid.models.User
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(private val onChatClick: (Chat) -> Unit) :
    ListAdapter<Chat, ChatListAdapter.ChatViewHolder>(ChatDiffCallback) {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPartnerName: TextView = itemView.findViewById(R.id.tvPartnerName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val ivPartnerAvatar: ImageView = itemView.findViewById(R.id.ivPartnerAvatar)

        fun bind(chat: Chat) {
            val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
            val partnerId = chat.userIds.firstOrNull { it != currentUserId }

            // Lấy thông tin người dùng đối diện
            Firebase.firestore.collection("users").document(partnerId!!).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val partner = document.toObject(User::class.java)
                        tvPartnerName.text = partner?.name ?: "Unknown"

                        // Hiển thị ảnh đại diện (nếu có)
                        partner?.profileImage?.let {
                            Glide.with(itemView).load(it).into(ivPartnerAvatar)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ChatListAdapter", "Error getting partner user", exception)
                }

            // Lấy tin nhắn cuối cùng và thời gian
            Firebase.firestore.collection("chats").document(chat.chatId).collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = getItem(position)
        holder.bind(chat)
        holder.itemView.setOnClickListener { onChatClick(chat) }
    }
}

object ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
    override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
        return oldItem.chatId == newItem.chatId
    }

    override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
        return oldItem == newItem
    }
}
