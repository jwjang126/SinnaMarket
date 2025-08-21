package com.motungi.sinnamarket.main

import android.location.Geocoder
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
    private var isMapReady = false // 지도 준비 상태를 추적할 변수
    private var isDataLoaded = false // 데이터 로드 상태를 추적할 변수
    private lateinit var naverMap: NaverMap

    private lateinit var voteOptionsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        photoRecyclerView = findViewById(R.id.detailPhotoRecyclerView)
        photoAdapter = PhotoAdapter(imageUrls)
        photoRecyclerView.adapter = photoAdapter
        photoRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        voteOptionsContainer = findViewById(R.id.voteOptionsContainer)

        val productId = intent.getStringExtra("productId")
        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "상품 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val distance = intent.getDoubleExtra("distance", -1.0)
        val distanceTextView = findViewById<TextView>(R.id.detaildistance)

        if (distance >= 0) {
            val formattedDistance = String.format("•  %.1f km", distance)
            distanceTextView.text = formattedDistance
        } else {
            distanceTextView.text = "거리 정보 없음"
        }

        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // 상품 데이터 가져오기
        db.collection("product").document(productId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val itemName = doc.getString("title") ?: ""
                val itemDesc = doc.getString("description") ?: ""
                //파베에 아직 날짜가 안올라가서 생기는 문제...일단 price로
                //val date = doc.getLong("price") ?: 0
                val district = doc.getString("region.district") ?: ""
                val dong = doc.getString("region.dong") ?: ""
                val itemPrice = doc.getLong("price") ?: 0
                val images = doc.get("imageUrls") as? List<String> ?: listOf()
                val uris = images.map { Uri.parse(it) }
                val isAvailable = doc.getBoolean("re_location") ?: false

                val locationMap = doc.get("location") as? Map<String, Any>
                val lat = locationMap?.get("lat") as? Double
                val lng = locationMap?.get("lng") as? Double
                val detailedDesc = locationMap?.get("desc") as? String ?: ""

                if (lat != null && lng != null) {
                    this.postLat = lat
                    this.postLng = lng
                    isDataLoaded = true
                    setupMapIfNeeded()
                    val fullAddress = getAddressFromLatLng(lat, lng)
                    findViewById<TextView>(R.id.detailadress).text = fullAddress
                }

                // 사진 리스트 세팅
                imageUrls.clear()
                imageUrls.addAll(uris)
                photoAdapter.notifyDataSetChanged()

                val authorId = doc.getString("authorid") ?: ""

                findViewById<TextView>(R.id.detailItemName).text = itemName
                findViewById<TextView>(R.id.detailItemDesc).text = itemDesc
                findViewById<TextView>(R.id.detailItemPrice).text = "$itemPrice 원"
                if(isAvailable){
                    findViewById<TextView>(R.id.detailrelocation).text = "위치 조율 가능"
                }
                else{
                    findViewById<TextView>(R.id.detailrelocation).text = "위치 조율 불가능"
                }
                //findViewById<TextView>(R.id.detaildate).text = "$date 원"
                findViewById<TextView>(R.id.detailadress2).text = detailedDesc



                // 작성자 정보
                if (authorId.isNotEmpty()) {
                    db.collection("users").document(authorId).get()
                        .addOnSuccessListener { userDoc ->
                            val authorName = userDoc.getString("nickname") ?: "Unknown"
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

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        isMapReady = true
        setupMapIfNeeded()
    }

    // 지도와 데이터가 모두 준비되었을 때만 호출되는 함수
    private fun setupMapIfNeeded() {
        if (isMapReady && isDataLoaded) {
            val postLocation = LatLng(postLat, postLng)
            val cameraUpdate = CameraUpdate.scrollTo(postLocation).animate(CameraAnimation.Easing)
            naverMap.moveCamera(cameraUpdate)

            val marker = Marker()
            marker.position = postLocation
            marker.map = naverMap
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    private fun getAddressFromLatLng(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this, Locale.KOREA)
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) ?: "주소 미상"
            } else {
                "주소 정보 없음"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "주소 변환 실패"
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
                    val voteCountText = optionLayout.findViewById<TextView>(R.id.voteCountText)

                    timeText.text = timeStr
                    voteCountText.text = "투표 수: ${voters.size}"

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
