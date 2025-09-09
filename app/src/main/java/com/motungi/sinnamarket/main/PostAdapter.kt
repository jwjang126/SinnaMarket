package com.motungi.sinnamarket.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.databinding.ItemPostBinding
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import android.view.View
import com.google.firebase.Timestamp


data class PostWithDistance(
    val post: Post,
    val distance: Double
)

class PostAdapter(private var postsWithDistance: List<PostWithDistance>, private val onItemClick: (PostWithDistance) -> Unit) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    fun updatePosts(newPostsWithDistance: List<PostWithDistance>) {
        postsWithDistance = newPostsWithDistance
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val item = postsWithDistance[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = postsWithDistance.size

    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val item = postsWithDistance[adapterPosition]
                onItemClick(item)
            }
        }
        fun bind(item: PostWithDistance) {
            val post = item.post

            // 게시글 속성을 화면에 표시
            binding.postTitle.text = post.title
            binding.postPrice.text = "${post.price} 원"

            val distance = item.distance
            val distanceText = if (distance < 1.0) {
                val distanceInMeters = distance * 1000
                String.format("• %.0f m", distanceInMeters)
            } else {
                String.format("• %.1f km", distance)
            }

            val dongName = post.region["dong"] ?: ""
            val guName = post.region["district"] ?: ""
            val timeAgo = getTimeAgo(post.uploadedAt)

            // 위치 및 거리 정보 표시
            binding.postLocation.text = "$guName $dongName $distanceText • $timeAgo"

            binding.postCategory.text = post.category

            val firstImageUrl = post.imageUrls.firstOrNull()
            if (!firstImageUrl.isNullOrEmpty()) {
                Glide.with(binding.postImage.context)
                    .load(firstImageUrl)
                    .centerCrop()
                    .into(binding.postImage)
            } else {
                binding.postImage.setImageResource(R.drawable.ic_launcher_background)
            }

            // post.state가 true면 '모집 완료' 텍스트를 보이도록 설정
            if (post.state) {
                binding.postState.visibility = View.VISIBLE
            } else {
                binding.postState.visibility = View.GONE
            }
        }

        private fun getTimeAgo(timestamp: Timestamp?): String {
            if (timestamp == null) return "날짜 정보 없음"
            val timeInMillis = timestamp.toDate().time  // Timestamp → Long(ms)
            val now = System.currentTimeMillis()
            val diff = now - timeInMillis

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