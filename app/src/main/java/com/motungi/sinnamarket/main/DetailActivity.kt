package com.motungi.sinnamarket.main

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import java.util.Calendar
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
import com.motungi.sinnamarket.auth.PhotoAdapter
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import java.util.Locale

class DetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private val imageUrls = mutableListOf<Uri>()

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var voteListener: ListenerRegistration? = null

    private lateinit var mapView: MapView
    private var postLat: Double = 0.0
    private var postLng: Double = 0.0
    private var isMapReady = false
    private var isDataLoaded = false
    private lateinit var naverMap: NaverMap

    private lateinit var voteOptionsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // RecyclerView 가로 스크롤
        photoRecyclerView = findViewById(R.id.detailPhotoRecyclerView)
        photoAdapter = PhotoAdapter(imageUrls)
        photoRecyclerView.adapter = photoAdapter
        photoRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photoRecyclerView.adapter = photoAdapter
        photoRecyclerView.setHasFixedSize(true) // 성능 최적화

        voteOptionsContainer = findViewById(R.id.voteOptionsContainer)

        val productId = intent.getStringExtra("productId") ?: run {
            Toast.makeText(this, "상품 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val distance = intent.getDoubleExtra("distance", -1.0)
        val distanceTextView = findViewById<TextView>(R.id.detaildistance)

        distanceTextView.text = if (distance >= 0) {
            if (distance < 1.0) {
                val distanceInMeters = distance * 1000
                String.format("현재 위치에서 %.0f m", distanceInMeters)
            } else {
                String.format("현재 위치에서 %.1f km", distance)
            }
        } else {
            "거리 정보 없음"
        }

        // MapView 초기화
        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // 상품 데이터 가져오기
        db.collection("product").document(productId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val itemName = doc.getString("title") ?: ""
                val itemDesc = doc.getString("description") ?: ""
                val year = doc.getLong("date.year") ?: 0
                val month = doc.getLong("date.month") ?: 0
                val day = doc.getLong("date.day") ?: 0
                val itemPrice = doc.getLong("price") ?: 0
                val images = doc.get("imageUrls") as? List<String> ?: listOf()
                val uris = images.map { Uri.parse(it) }
                val isAvailable = doc.getBoolean("re_location") ?: false
                val locationMap = doc.get("location") as? Map<String, Any>
                val lat = (locationMap?.get("lat") as? Number)?.toDouble()
                val lng = (locationMap?.get("lng") as? Number)?.toDouble()


                val detailedDesc = locationMap?.get("desc") as? String ?: ""
                val authorId = doc.getString("authorid") ?: ""
                val numPeople = doc.getLong("numPeople") ?: 0
                val state = doc.getBoolean("state") ?: false
                val timestamp = doc.getTimestamp("uploadedAt")
                val date = timestamp?.toDate()


                if (lat != null && lng != null) {
                    this.postLat = lat
                    this.postLng = lng
                    isDataLoaded = true
                    setupMapIfNeeded()
                    findViewById<TextView>(R.id.detailadress).text = getAddressFromLatLng(lat, lng)
                }

                if (date != null) {
                    val calendar = Calendar.getInstance().apply {
                        time = date
                    }
                    val wyear = calendar.get(Calendar.YEAR)
                    val wmonth = calendar.get(Calendar.MONTH) + 1 // 월은 0부터 시작하니까 +1
                    val wday = calendar.get(Calendar.DAY_OF_MONTH)

                    findViewById<TextView>(R.id.detailwritedate).text =
                        "작성일: $wyear 년 $wmonth 월 $wday 일"
                } else {
                    findViewById<TextView>(R.id.detailwritedate).text = "작성일 정보 없음"
                }

                // RecyclerView에 이미지 URL 반영
                imageUrls.clear()
                imageUrls.addAll(uris)
                photoAdapter.notifyDataSetChanged()

                // 텍스트뷰 설정
                findViewById<TextView>(R.id.detailItemName).text = itemName
                findViewById<TextView>(R.id.detailItemDesc).text = itemDesc
                findViewById<TextView>(R.id.detailItemPrice).text = "$itemPrice 원"
                findViewById<TextView>(R.id.detailrelocation).text = if (isAvailable) "위치 조율 가능" else "위치 조율 불가능"
                findViewById<TextView>(R.id.detaildate).text = "거래 희망 날짜: $year 년 $month 월 $day 일"
                findViewById<TextView>(R.id.detailadress2).text = detailedDesc
                findViewById<TextView>(R.id.detailNumPeople).text = "모집 인원: ${numPeople}명"

                // 작성자 정보 가져오기
                if (authorId.isNotEmpty()) {
                    db.collection("users").document(authorId).get()
                        .addOnSuccessListener { userDoc ->
                            val authorName = userDoc.getString("nickname") ?: "Unknown"
                            val rating = userDoc.getDouble("rating") ?: 0.0
                            findViewById<TextView>(R.id.detailAuthorInfo).text =
                                "작성자: $authorName (평점: $rating)"
                        }
                }

                listenVoteOptions(productId, numPeople, state)
            }
            .addOnFailureListener { Log.e("DetailActivity", "상품 불러오기 실패: ${it.message}") }
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        isMapReady = true
        setupMapIfNeeded()
    }

    private fun setupMapIfNeeded() {
        if (isMapReady && isDataLoaded) {
            val postLocation = LatLng(postLat, postLng)
            naverMap.moveCamera(CameraUpdate.scrollTo(postLocation).animate(CameraAnimation.Easing))
            Marker().apply { position = postLocation; map = naverMap }
        }
    }

    private fun getAddressFromLatLng(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this, Locale.KOREA)
        return try {
            geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0) ?: "주소 정보 없음"
        } catch (e: Exception) { e.printStackTrace(); "주소 변환 실패" }
    }

    private fun listenVoteOptions(productId: String, numPeople: Long,state: Boolean) {
        voteListener?.remove()
        val voteOptionsRef = db.collection("product").document(productId).collection("voteOptions")
        voteListener = voteOptionsRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            voteOptionsContainer.removeAllViews()

            for (optionDoc in snapshot.documents) {
                val optionId = optionDoc.id
                val timeStr = optionDoc.getString("time") ?: ""

                val inflater = LayoutInflater.from(this)
                val optionLayout = inflater.inflate(R.layout.vote_option_item, voteOptionsContainer, false)
                val timeText = optionLayout.findViewById<TextView>(R.id.voteTimeText)
                val voteButton = optionLayout.findViewById<Button>(R.id.voteButton)
                val qtyInput = optionLayout.findViewById<EditText>(R.id.voteQuantityInput)
                val voteCountText = optionLayout.findViewById<TextView>(R.id.voteCountText)

                timeText.text = timeStr

                val votersRef = voteOptionsRef.document(optionId).collection("voters")

                // 실시간으로 투표 상태 반영
                votersRef.addSnapshotListener { votersSnap, _ ->
                    var totalVotes = 0
                    var myVoteQty = 0
                    for (voterDoc in votersSnap?.documents ?: listOf()) {
                        val qty = voterDoc.getLong("qty")?.toInt() ?: 0
                        totalVotes += qty
                        if (voterDoc.id == currentUser?.uid) myVoteQty = qty
                    }

                    voteCountText.text = "투표 수: $totalVotes"
                    qtyInput.setText(myVoteQty.toString())

                    // ✅ state가 true면 비활성화, false면 항상 클릭 가능
                    voteButton.isEnabled = !state
                    qtyInput.isEnabled = !state
                    voteButton.text = if (myVoteQty > 0) "다시 투표하기" else "투표"
                }

                voteButton.setOnClickListener {
                    val qty = qtyInput.text.toString().toIntOrNull() ?: 0
                    val userId = currentUser?.uid ?: return@setOnClickListener
                    val voterRef = votersRef.document(userId)
                    val optionRef = voteOptionsRef.document(optionId)

                    // 🔹 product 문서 먼저 확인
                    db.collection("product").document(productId).get()
                        .addOnSuccessListener { productSnap ->
                            val productState = productSnap.getBoolean("state") ?: false
                            val numPeople = productSnap.getLong("numPeople") ?: Long.MAX_VALUE

                            // 이미 종료된 경우
                            if (productState) {
                                Toast.makeText(this, "이미 투표가 종료되었습니다.", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // 트랜잭션 실행
                            db.runTransaction { transaction ->
                                val optionSnap = transaction.get(optionRef)
                                val currentTotal = optionSnap.getLong("total") ?: 0
                                val voterSnap = transaction.get(voterRef)
                                val prevQty = voterSnap.getLong("qty") ?: 0
                                val newTotal = currentTotal - prevQty + qty

                                // 🔹 numPeople 넘으면 차단
                                if (newTotal > numPeople) {
                                    throw Exception("인원 제한 초과")
                                }

                                if (qty > 0) transaction.set(voterRef, mapOf("qty" to qty))
                                else transaction.delete(voterRef)

                                transaction.update(optionRef, "total", newTotal)
                            }.addOnSuccessListener {
                                optionRef.get().addOnSuccessListener { optionSnap ->
                                    val total = optionSnap.getLong("total") ?: 0L

                                    // 🔹 투표 종료 조건 확인
                                    if (total >= numPeople && !productState) {
                                        // product.state 업데이트
                                        db.collection("product").document(productId)
                                            .update("state", true)

                                        // 채팅방 생성
                                        votersRef.get().addOnSuccessListener { votersSnap ->
                                            val participantIds = votersSnap.documents.map { it.id }.toMutableList()
                                            val authorId = productSnap.getString("authorid")
                                            if (!authorId.isNullOrEmpty()) participantIds.add(authorId)

                                            val chatRoomMembers = participantIds.distinct()
                                            val chatRoomRef = db.collection("chats").document()
                                            chatRoomRef.set(
                                                mapOf(
                                                    "productId" to productId,
                                                    "participants" to chatRoomMembers
                                                )
                                            ).addOnSuccessListener {
                                                val messagesRef = chatRoomRef.collection("messages")
                                                messagesRef.add(
                                                    mapOf(
                                                        "senderId" to "system",
                                                        "text" to "투표가 종료되었습니다. 채팅방이 생성되었습니다.",
                                                        "createdAt" to System.currentTimeMillis()
                                                    )
                                                )
                                                chatRoomMembers.forEach { userId ->
                                                    db.collection("users").document(userId)
                                                        .update("chatRooms", com.google.firebase.firestore.FieldValue.arrayUnion(chatRoomRef.id))
                                                }

                                                val intent = Intent(this@DetailActivity, ChatroomActivity::class.java)
                                                intent.putExtra("chatRoomId", chatRoomRef.id)
                                                startActivity(intent)
                                            }
                                        }
                                    }
                                }
                            }.addOnFailureListener { e ->
                                if (e.message?.contains("인원 제한 초과") == true) {
                                    Toast.makeText(this, "투표 인원이 이미 가득 찼습니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.e("DetailActivity", "투표 실패: ${e.message}")
                                }
                            }
                        }
                }

                voteOptionsContainer.addView(optionLayout)
            }
        }
    }


    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroy() { super.onDestroy(); voteListener?.remove(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}
