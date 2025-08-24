package com.motungi.sinnamarket.auth

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.motungi.sinnamarket.R

class PhotoAdapter(private var photos:List<Uri>) :
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.photoItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        Glide.with(holder.imageView.context)
            .load(photos[position]) // URL 문자열로 로드
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = photos.size

    fun updatePhotos(newPhotos: List<Uri>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
}
