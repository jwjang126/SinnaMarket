package com.motungi.sinnamarket.main

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.motungi.sinnamarket.R

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0
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
                    val timestamp = doc.getLong("timestamp") ?: 0
                    messages.add(ChatMessage(senderId, text, timestamp))
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
}
