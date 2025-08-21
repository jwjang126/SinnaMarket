package com.motungi.sinnamarket.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryName = it.getString(ARG_CATEGORY_NAME)
            regionName = it.getString(ARG_REGION_NAME)
        }
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

    /**
     * 외부에서 새로운 지역 정보를 받아서 데이터를 새로고침하는 함수
     */
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

                            // test
                            val userLat = 35.888   // 경북대 근처 위도
                            val userLng = 128.610  // 경북대 근처 경도

                            val distance = calculateDistance(userLat, userLng, lat, lng)
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

    private fun calculateDistance(
        userLat: Double,
        userLng: Double,
        postLat: Double,
        postLng: Double
    ): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(postLat - userLat)
        val dLng = Math.toRadians(postLng - userLng)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(postLat)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c // km 단위 결과 반환
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