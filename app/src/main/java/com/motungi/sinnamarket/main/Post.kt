package com.motungi.sinnamarket.main

// Firebase Firestore에서 게시글 데이터를 담을 데이터 클래스
data class Post(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val price: Int = 0,
    val location: String = "",
    val region: String = "", // 지역 정보 추가
    val timestamp: Long = 0L
)
