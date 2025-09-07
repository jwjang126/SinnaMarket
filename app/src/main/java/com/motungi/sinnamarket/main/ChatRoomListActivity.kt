package com.motungi.sinnamarket.main

import ChatRoom
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.motungi.sinnamarket.R

class ChatRoomListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val chatRooms = mutableListOf<ChatRoom>()
    private lateinit var adapter: ChatRoomAdapter
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatroom_list)

        recyclerView = findViewById(R.id.chatRoomRecyclerView)
        adapter = ChatRoomAdapter(chatRooms, currentUserId) { chatRoomId ->
            // 해당 채팅방으로 이동
            val intent = Intent(this, ChatroomActivity::class.java)
            intent.putExtra("chatRoomId", chatRoomId)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadChatRooms()

        // BottomNavigationView 가져오기
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_bar)
        bottomNav.selectedItemId = R.id.nav_chat // 현재 선택 상태

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_chat -> {
                    // 현재 화면이니까 아무 행동 없음
                    true
                }
                R.id.nav_my_info -> {
                    val intent = Intent(this, MyInfoActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadChatRooms() {
        val db = FirebaseFirestore.getInstance()
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { docs ->
                chatRooms.clear()
                for (doc in docs) {
                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    val itemId = doc.getString("itemId") ?: ""

                    // 마지막 메시지 가져오기
                    doc.reference.collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { msgDocs ->
                            var lastMsg = ""
                            var lastTime = 0L
                            var lastSender = ""
                            if (!msgDocs.isEmpty) {
                                val msg = msgDocs.documents[0]
                                lastMsg = msg.getString("text") ?: ""
                                lastTime = msg.getLong("timestamp") ?: 0L
                                lastSender = msg.getString("senderId") ?: ""
                            }
                            chatRooms.add(ChatRoom(doc.id, participants, itemId, lastMsg, lastTime, lastSender))
                            adapter.notifyDataSetChanged()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatRoomListActivity", "채팅방 불러오기 실패", e)
            }
    }
}
