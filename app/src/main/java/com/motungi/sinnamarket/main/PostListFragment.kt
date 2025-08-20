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
import com.motungi.sinnamarket.main.DetailActivity // DetailActivity import 추가

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

        // PostAdapter를 초기화할 때 클릭 리스너를 전달합니다.
        postAdapter = PostAdapter(emptyList()) { post ->
            // PostListFragment.kt:71
            val intent = Intent(context, DetailActivity::class.java)
            // post 객체에 id 필드가 없으므로, Firestore의 문서 ID를 직접 전달해야 합니다.
            // 이 로직은 `listenForPosts`에서 snapshot을 가져올 때 함께 저장되어야 합니다.
            intent.putExtra("productId", post.productid)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = postAdapter

        listenForPosts()
    }

    private fun listenForPosts() {
        if (categoryName == null || regionName == null) {
            Log.e("PostListFragment", "Category or Region name is null.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("product") // Firebase 데이터베이스 컬렉션 이름과 일치하도록 수정
            .whereEqualTo("category", categoryName)
            .whereEqualTo("region.dong", regionName) // 지역별 필터링
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("PostListFragment", "Error listening for posts", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val posts = snapshots.map { doc ->
                        val post = doc.toObject(Post::class.java)
                        post.copy(productid = doc.id)  // 🔹 문서 ID를 Post 객체에 저장
                    }
                    postAdapter.updatePosts(posts)

                    Log.d("PostListFragment", "Loaded ${posts.size} posts, first productId: ${posts.firstOrNull()?.productid}")
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
