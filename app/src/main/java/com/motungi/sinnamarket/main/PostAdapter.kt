package com.motungi.sinnamarket.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.databinding.ItemPostBinding
import java.util.concurrent.TimeUnit

class PostAdapter(private var posts: List<Post>, private val onItemClick: (Post) -> Unit) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int = posts.size

    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val post = posts[adapterPosition]
                onItemClick(post)
            }
        }
        fun bind(post: Post) {
            // 게시글 속성을 화면에 표시
            binding.postTitle.text = post.title
            binding.postPrice.text = "${post.price} 원"

            // region 맵에서 'dong' 값을 가져와서 위치로 표시
            val dongName = post.region["dong"] ?: ""
            // uploadedAt 시간을 "N시간 전" 형식으로 변환하여 표시
            val timeAgo = getTimeAgo(post.uploadedAt)
            binding.postLocation.text = "$dongName • $timeAgo"

            binding.postCategory.text = post.category

            // imageUrls 리스트의 첫 번째 이미지를 로드
            val firstImageUrl = post.imageUrls.firstOrNull()
            if (!firstImageUrl.isNullOrEmpty()) {
                Glide.with(binding.postImage.context)
                    .load(firstImageUrl)
                    .centerCrop()
                    .into(binding.postImage)
            } else {
                binding.postImage.setImageResource(R.drawable.ic_launcher_background)
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                seconds < 60 -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                hours < 24 -> "${hours}시간 전"
                days < 7 -> "${days}일 전"
                else -> "오래 전"
            }
        }
    }
}
