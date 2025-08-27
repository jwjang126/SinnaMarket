package com.motungi.sinnamarket.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.databinding.ActivitySearchBinding
import androidx.appcompat.widget.SearchView
import android.view.MenuItem
import android.location.Location

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var searchView: SearchView
    private var searchQuery: String? = null
    private var userLat: Double = 35.888 // 하드코딩된 사용자 위치 (테스트용)
    private var userLng: Double = 128.610 // 하드코딩된 사용자 위치 (테스트용)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "검색 결과"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        searchQuery = intent.getStringExtra("search_query")

        postAdapter = PostAdapter(emptyList()) { postWithDistance ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("productId", postWithDistance.post.id)
            intent.putExtra("distance", postWithDistance.distance)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = postAdapter

        if (!searchQuery.isNullOrBlank()) {
            fetchSearchResults(searchQuery!!)
        } else {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.tvNoResults.text = "검색어를 입력해 주세요."
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchSearchResults(query: String) {
        val db = FirebaseFirestore.getInstance()

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val selectedRegion = prefs.getString("selected_region", "수성구") ?: "수성구"

        // Firestore에서 지역에 해당하는 모든 게시물을 가져옵니다.
        db.collection("product")
            .whereEqualTo("region.dong", selectedRegion)
            .get()
            .addOnSuccessListener { snapshots ->
                val postsWithDistance = snapshots.documents.mapNotNull { doc ->
                    val post = doc.toObject(Post::class.java)?.copy(id = doc.id)
                    post?.let {
                        val lat = (it.location["lat"] ?: 0.0) as Double
                        val lng = (it.location["lng"] ?: 0.0) as Double
                        val userLat = 35.888
                        val userLng = 128.610
                        val distance = calculateDistance(userLat, userLng, lat, lng)
                        PostWithDistance(it, distance)
                    }
                }

                // 앱 내부에서 제목에 검색어가 포함된 게시물만 필터링합니다.
                val filteredPosts = postsWithDistance.filter {
                    it.post.title.contains(query, ignoreCase = true)
                }

                // 필터링된 결과를 최신순으로 정렬합니다.
                val sortedPosts = filteredPosts.sortedByDescending { it.post.uploadedAt }

                postAdapter.updatePosts(sortedPosts)

                if (sortedPosts.isEmpty()) {
                    binding.tvNoResults.visibility = View.VISIBLE
                    binding.tvNoResults.text = "'$query'에 대한 검색 결과가 없습니다."
                } else {
                    binding.tvNoResults.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e("SearchActivity", "Error fetching search results", e)
                binding.tvNoResults.visibility = View.VISIBLE
                binding.tvNoResults.text = "검색 중 오류가 발생했습니다."
            }
    }

    private fun calculateDistance(
        userLat: Double,
        userLng: Double,
        postLat: Double,
        postLng: Double
    ): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(postLat - userLat)
        val dLng = Math.toRadians(postLng - userLng)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(postLat)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}