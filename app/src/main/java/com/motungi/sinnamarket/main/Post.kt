package com.motungi.sinnamarket.main

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.*

data class Post(
    val authorid: String = "",
    val category: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val location: Map<String, Any> = mapOf(),
    val price: Int = 0,
    val region: Map<String, String> = mapOf(),
    val title: String = "",
    val uploadedAt: Long = 0L,
    val state: Boolean = false, // '모집 완료' 상태를 위한 필드 추가
    @get:Exclude
    var id: String = "" // Firestore 문서 ID를 저장할 필드 추가 (DB에 저장되지 않음)
)

data class VoteOption(
    val time: String = "",
    val voters: List<String> = emptyList()
)