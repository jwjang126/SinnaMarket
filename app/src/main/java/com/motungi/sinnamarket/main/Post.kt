package com.motungi.sinnamarket.main

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.*
import com.google.firebase.Timestamp
import java.util.Date

data class Post(
    val authorid: String = "",
    val category: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val location: Map<String, Any> = mapOf(),
    val price: Int = 0,
    val region: Map<String, String> = mapOf(),
    val title: String = "",
    val uploadedAt: Timestamp = Timestamp.now(), // timestamp로 변경
    val state: Boolean = false, // '모집 완료' 상태
    @get:Exclude
    var id: String = "" // Firestore 문서 ID
)

data class VoteOption(
    val time: String = "",
    val voters: List<String> = emptyList()
)