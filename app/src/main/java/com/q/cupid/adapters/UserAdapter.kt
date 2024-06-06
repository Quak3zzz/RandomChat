package com.q.cupid.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.q.cupid.R
import com.q.cupid.models.User

class UserAdapter(private val onUserClick: (User) -> Unit) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback) {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(user: User) {
                // Tìm các View trong hàm bind
                val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
                val ivAvatar: ImageView = itemView.findViewById(R.id.ivPartnerAvatar)
                tvUserName.text = user.name

            // Hiển thị ảnh đại diện (nếu có)
            Glide.with(itemView)
                .load(user.profileImage)
                .placeholder(R.drawable.default_avatar) // Ảnh mặc định khi chưa tải được
                .error(R.drawable.default_avatar) // Ảnh mặc định khi tải lỗi
                .into(ivAvatar)

            itemView.setOnClickListener { onUserClick(user) } // Xử lý sự kiện click vào item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }
}

object UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}
