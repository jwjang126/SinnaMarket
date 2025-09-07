package com.motungi.sinnamarket.main

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.auth.RatingManager

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    var senderNickname: String = "" // 추가
)


class ChatroomActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendBtn: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var chatRoomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatroom)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendBtn = findViewById(R.id.sendBtn)

        chatAdapter = ChatAdapter(messages, currentUser!!.uid)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        chatRoomId = intent.getStringExtra("chatRoomId")
        if (chatRoomId == null) finish() // chatRoomId 없으면 종료

        listenMessages()

        sendBtn.setOnClickListener {
            val msg = messageInput.text.toString()
            if (msg.isNotBlank() && currentUser != null && chatRoomId != null) {
                sendMessage(msg)
                messageInput.text.clear()
            }
        }
    }

    private fun listenMessages() {
        db.collection("chats")
            .document(chatRoomId!!)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                messages.clear()
                for (doc in snapshot.documents) {
                    val text = doc.getString("text") ?: ""
                    val senderId = doc.getString("senderId") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L

                    val chatMessage = ChatMessage(senderId, text, timestamp)

                    // 닉네임 가져오기
                    if (senderId.isNotEmpty()) {
                        db.collection("users").document(senderId).get()
                            .addOnSuccessListener { userDoc ->
                                chatMessage.senderNickname = userDoc.getString("nickname") ?: "Unknown"
                                chatAdapter.notifyDataSetChanged()
                            }
                    }

                    messages.add(chatMessage)
                }
                chatAdapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
    }


    private fun sendMessage(msg: String) {
        val messageData = hashMapOf(
            "text" to msg,
            "senderId" to currentUser!!.uid,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("chats")
            .document(chatRoomId!!)
            .collection("messages")
            .add(messageData)
    }

    // 평점
    fun showRatingDialog(ratedUid: String, nickname: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating,null)

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val reasonEditText = dialogView.findViewById<EditText>(R.id.reason_edit_text)
        val nicknameTextView = dialogView.findViewById<TextView>(R.id.nickname_text_view)

        nicknameTextView.text = "'${nickname}'님을 평가해주세요"

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("제출"){ dialog, _ ->
                val score = ratingBar.rating
                val reason = reasonEditText.text.toString()
                val raterUid = currentUser?.uid

                if(raterUid != null && score>0) {
                    RatingManager.submitRatingToFirebase(ratedUid, raterUid, score, reason)
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소"){ dialog, _->
                dialog.dismiss()
            }
        builder.show()
    }
}
