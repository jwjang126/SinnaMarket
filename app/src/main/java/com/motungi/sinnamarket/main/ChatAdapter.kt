package com.motungi.sinnamarket.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.motungi.sinnamarket.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ME = 1
    private val VIEW_TYPE_OTHER = 2

    // 사용자 캐시: senderId -> nickname
    private val userCache = mutableMapOf<String, String>()

    // 시간 포맷 미리 생성
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class MyMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timestamp: TextView = view.findViewById(R.id.messageTime)
    }

    inner class OtherMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val nickname: TextView = view.findViewById(R.id.nicknameText)
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val timestamp: TextView = view.findViewById(R.id.messageTime)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_ME else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ME) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_my, parent, false)
            MyMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_other, parent, false)
            OtherMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val timeStr = timeFormat.format(Date(msg.timestamp))

        if (holder is MyMessageViewHolder) {
            holder.messageText.text = msg.text
            holder.timestamp.text = timeStr
        } else if (holder is OtherMessageViewHolder) {
            holder.messageText.text = msg.text
            holder.timestamp.text = timeStr

            // 캐시에 있으면 바로 사용
            val cachedNickname = userCache[msg.senderId]
            if (cachedNickname != null) {
                holder.nickname.text = cachedNickname
            } else {
                // Firestore에서 nickname 가져오기
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users").document(msg.senderId).get()
                    .addOnSuccessListener { doc ->
                        val nickname = doc.getString("nickname") ?: "익명"
                        holder.nickname.text = nickname
                        userCache[msg.senderId] = nickname
                    }
                    .addOnFailureListener {
                        holder.nickname.text = "익명"
                    }
            }

            holder.profileImage.setImageResource(R.drawable.ic_default_profile)
            holder.profileImage.setOnClickListener{
                val context = holder.itemView.context
                if(context is ChatroomActivity){
                    val nicknameToShow = holder.nickname.text.toString()
                    context.showRatingDialog(msg.senderId, nicknameToShow)
                }
            }
        }
    }
    override fun getItemCount(): Int = messages.size
}

