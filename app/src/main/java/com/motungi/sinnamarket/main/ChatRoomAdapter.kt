package com.motungi.sinnamarket.main

import ChatRoom
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.motungi.sinnamarket.R
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomAdapter(
    private val chatRooms: List<ChatRoom>,
    private val currentUserId: String,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    private val userCache = mutableMapOf<String, String>() // uid -> nickname 캐시
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val db = FirebaseFirestore.getInstance()

    inner class ChatRoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomNameText: TextView = view.findViewById(R.id.chatRoomNameTextView)
        val lastMessageText: TextView = view.findViewById(R.id.lastMessageTextView)
        val timestampText: TextView = view.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_room, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val room = chatRooms[position]

        // 상대방 ID 가져오기 (본인 제외)
        val otherUserId = room.participants.firstOrNull { it != currentUserId } ?: "Unknown"

        // 캐시에서 바로 가져오기
        val cachedName = userCache[otherUserId]
        if (cachedName != null) {
            holder.roomNameText.text = cachedName
        } else {
            // Firestore에서 nickname 가져오기
            db.collection("users").document(otherUserId).get()
                .addOnSuccessListener { doc ->
                    val nickname = doc.getString("nickname") ?: otherUserId.take(6)
                    holder.roomNameText.text = nickname
                    userCache[otherUserId] = nickname
                }
                .addOnFailureListener {
                    holder.roomNameText.text = otherUserId.take(6)
                }
        }

        holder.lastMessageText.text = room.lastMessage
        holder.timestampText.text = if (room.lastMessageTime != 0L) {
            timeFormat.format(Date(room.lastMessageTime))
        } else ""

        holder.itemView.setOnClickListener {
            onClick(room.id)
        }
    }

    override fun getItemCount() = chatRooms.size
}
