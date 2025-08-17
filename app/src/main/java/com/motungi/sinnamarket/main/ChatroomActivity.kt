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
    val text: String = ""
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

        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

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
        db.collection("chatRooms")
            .document(chatRoomId!!)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                messages.clear()
                for (doc in snapshot.documents) {
                    val text = doc.getString("text") ?: ""
                    val senderId = doc.getString("senderId") ?: ""
                    messages.add(ChatMessage(senderId, text))
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
        db.collection("chatRooms")
            .document(chatRoomId!!)
            .collection("messages")
            .add(messageData)
    }
}
