package com.motungi.sinnamarket.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.motungi.sinnamarket.databinding.FragmentPostListBinding
import com.motungi.sinnamarket.main.DetailActivity
import com.motungi.sinnamarket.main.Post
import com.motungi.sinnamarket.main.PostAdapter
import com.motungi.sinnamarket.R

class PostListFragment : Fragment() {

    private var _binding: FragmentPostListBinding? = null
    private val binding get() = _binding!!
    private var categoryName: String? = null
    private var regionName: String? = null
    private lateinit var postAdapter: PostAdapter

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLat: Double = 0.0
    private var userLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryName = it.getString(ARG_CATEGORY_NAME)
            regionName = it.getString(ARG_REGION_NAME)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        // 위치 권한 요청
        requestLocationPermission()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // PostAdapter 초기화 시 클릭 리스너에서 post.id를 사용하도록 수정
        postAdapter = PostAdapter(emptyList()) { postWithDistance ->
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("productId", postWithDistance.post.id)
            intent.putExtra("distance", postWithDistance.distance)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = postAdapter

        // tvNoPosts는 View Binding으로 접근하므로 별도의 선언이 필요 없습니다.
        // 초기에는 보이지 않도록 설정
        binding.tvNoPosts.visibility = View.GONE

        listenForPosts()
    }

    fun updateRegion(newRegion: String) {
        if (this.regionName != newRegion) {
            this.regionName = newRegion
            listenForPosts() // 지역이 바뀌면 Firebase 쿼리를 다시 실행합니다.
        }
    }

    private fun listenForPosts() {
        if (categoryName == null || regionName == null) {
            Log.e("PostListFragment", "Category or Region name is null.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("product")
            .whereEqualTo("category", categoryName)
            .whereEqualTo("region.dong", regionName)
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("PostListFragment", "Error listening for posts", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val postsWithDistance = snapshots.documents.mapNotNull { doc ->
                        val post = doc.toObject(Post::class.java)?.copy(id = doc.id)
                        post?.let {
                            val lat = (it.location["lat"] ?: 0.0) as Double
                            val lng = (it.location["lng"] ?: 0.0) as Double

                            val distance = if (userLat != 0.0 && userLng != 0.0) {
                                calculateDistance(userLat, userLng, lat, lng)
                            } else {
                                0.0
                            }
                            PostWithDistance(it, distance)
                        }
                    }

                    postAdapter.updatePosts(postsWithDistance)
                    Log.d("PostListFragment", "Loaded ${postsWithDistance.size} posts in real-time.")

                    if (postsWithDistance.isEmpty()) {
                        binding.tvNoPosts.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.tvNoPosts.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getUserLocation()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getUserLocation()
            } else {
                // 사용자가 권한을 거부한 경우 처리
                Log.d("PostListFragment", "위치 권한이 거부되었습니다.")
                // 권한이 없으므로 userLat, userLng는 0.0으로 유지됩니다.
                // 이 상태로 listenForPosts()를 호출하면 거리는 계산되지 않습니다.
                listenForPosts()
            }
        }

    private fun getUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLat = location.latitude
                    userLng = location.longitude
                    // 위치를 가져온 후 게시글 목록을 다시 로드하여 거리를 업데이트
                    listenForPosts()
                } else {
                    Log.d("PostListFragment", "위치를 가져올 수 없습니다.")
                }
            }
        }
    }

    private fun calculateDistance(userLat: Double, userLng: Double, postLat: Double, postLng: Double): Double {
        val userLocation = Location("user").apply {
            latitude = userLat
            longitude = userLng
        }
        val postLocation = Location("post").apply {
            latitude = postLat
            longitude = postLng
        }

        val distanceInMeters = userLocation.distanceTo(postLocation)
        return distanceInMeters / 1000.0 // km 단위로 반환
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY_NAME = "category_name"
        private const val ARG_REGION_NAME = "region_name"

        fun newInstance(categoryName: String, regionName: String) =
            PostListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_NAME, categoryName)
                    putString(ARG_REGION_NAME, regionName)
                }
            }
    }
}