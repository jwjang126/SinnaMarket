package com.motungi.sinnamarket.auth

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.motungi.sinnamarket.R

class DetailActivity : AppCompatActivity() {

    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private val imageUrls = mutableListOf<Uri>()

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var voteListener: ListenerRegistration? = null

    private lateinit var voteOptionsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        photoRecyclerView = findViewById(R.id.detailPhotoRecyclerView)
        photoAdapter = PhotoAdapter(imageUrls)
        photoRecyclerView.adapter = photoAdapter
        photoRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        voteOptionsContainer = findViewById(R.id.voteOptionsContainer)

        val productId = intent.getStringExtra("productId") ?: return

        // 상품 데이터 가져오기
        db.collection("product").document(productId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val itemName = doc.getString("name") ?: ""
                val itemDesc = doc.getString("description") ?: ""
                val itemPrice = doc.getLong("price") ?: 0
                val images = doc.get("imageUrls") as? List<String> ?: listOf()
                val uris = images.map { Uri.parse(it) }

                // 사진 리스트 세팅
                imageUrls.clear()
                imageUrls.addAll(uris)
                photoAdapter.notifyDataSetChanged()

                val authorId = doc.getString("authorId") ?: ""

                findViewById<TextView>(R.id.detailItemName).text = itemName
                findViewById<TextView>(R.id.detailItemDesc).text = itemDesc
                findViewById<TextView>(R.id.detailItemPrice).text = "$itemPrice 원"

                // 작성자 정보
                if (authorId.isNotEmpty()) {
                    db.collection("users").document(authorId).get()
                        .addOnSuccessListener { userDoc ->
                            val authorName = userDoc.getString("name") ?: "Unknown"
                            val rating = userDoc.getDouble("rating") ?: 0.0
                            findViewById<TextView>(R.id.detailAuthorInfo).text =
                                "작성자: $authorName (평점: $rating)"
                        }
                }

                // 실시간 투표 반영
                listenVoteOptions(productId)
            }
            .addOnFailureListener {
                Log.e("DetailActivity", "상품 불러오기 실패: ${it.message}")
            }
    }

    private fun listenVoteOptions(productId: String) {
        voteListener?.remove() // 기존 리스너 제거
        voteListener = db.collection("product").document(productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val voteOptions = snapshot.get("voteOptions") as? List<Map<String, Any>> ?: listOf()
                voteOptionsContainer.removeAllViews()

                for ((index, option) in voteOptions.withIndex()) {
                    val timeStr = option["time"] as? String ?: ""
                    val voters = option["voters"] as? List<String> ?: listOf()

                    val inflater = LayoutInflater.from(this)
                    val optionLayout =
                        inflater.inflate(R.layout.vote_option_item, voteOptionsContainer, false)
                    val timeText = optionLayout.findViewById<TextView>(R.id.voteTimeText)
                    val voteButton = optionLayout.findViewById<Button>(R.id.voteButton)
                    val qtyInput = optionLayout.findViewById<EditText>(R.id.voteQuantityInput)

                    timeText.text = timeStr

                    val userId = currentUser?.uid
                    voteButton.isEnabled = userId != null && !voters.contains(userId)

                    voteButton.setOnClickListener {
                        val qty = qtyInput.text.toString().toIntOrNull() ?: 1
                        val updatedVoters = voters.toMutableList()
                        repeat(qty) { userId?.let { updatedVoters.add(it) } }

                        db.collection("product").document(productId)
                            .update("voteOptions.$index.voters", updatedVoters)
                            .addOnSuccessListener {
                                Toast.makeText(this, "투표 완료", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "투표 실패", Toast.LENGTH_SHORT).show()
                            }
                    }

                    voteOptionsContainer.addView(optionLayout)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        voteListener?.remove()
    }
}
