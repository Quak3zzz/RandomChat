package com.q.cupid.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.q.cupid.R
import com.q.cupid.models.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val currentUserId: String) :
    ListAdapter<Message, MessageAdapter.MessageViewHolder>(DiffCallback) {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val tvMessageTime: TextView = itemView.findViewById(R.id.tvMessageTime) // Thêm TextView cho thời gian
        // Thêm ImageView (hoặc View) để hiển thị trạng thái online/offline nếu cần
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.tvMessage.text = message.text

        // Hiển thị thời gian tin nhắn
        val messageTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
        holder.tvMessageTime.text = messageTime

        // Tùy chỉnh giao diện dựa trên người gửi
        if (message.senderId == currentUserId) {
            holder.tvMessage.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_message_sent)
            holder.tvMessage.textAlignment = View.TEXT_ALIGNMENT_TEXT_END // Căn phải
            val params = holder.tvMessage.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            holder.tvMessage.layoutParams = params

            // Ẩn thời gian tin nhắn của người gửi (tùy chọn)
            holder.tvMessageTime.visibility = View.GONE

        } else {
            holder.tvMessage.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_message_received)
            holder.tvMessageTime.textAlignment = View.TEXT_ALIGNMENT_TEXT_START // Căn trái
            val params = holder.tvMessage.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
            holder.tvMessage.layoutParams = params
        }

        // Cập nhật trạng thái online/offline (nếu cần)
        // ... (Thêm logic để cập nhật trạng thái của ImageView ở đây)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.messageId == newItem.messageId // So sánh dựa trên messageId
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

}
