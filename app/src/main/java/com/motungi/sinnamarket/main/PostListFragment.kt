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
        postAdapter = PostAdapter(emptyList()) { post ->
            val intent = Intent(context, DetailActivity::class.java)
            // Post 데이터 클래스에 id 필드를 추가했으므로, 이제 post.id를 사용합니다.
            intent.putExtra("productId", post.id)
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
                    // Firestore 문서 ID를 Post 객체에 할당하는 로직 추가
                    val posts = snapshots.documents.mapNotNull { doc ->
                        val post = doc.toObject(Post::class.java)
                        post?.copy(id = doc.id)
                    }
                    postAdapter.updatePosts(posts)
                    Log.d("PostListFragment", "Loaded ${posts.size} posts in real-time.")

                    // 게시글이 없을 때 메시지를 보여주는 로직 추가
                    if (posts.isEmpty()) {
                        binding.tvNoPosts.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.tvNoPosts.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
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