package com.motungi.sinnamarket.main

data class Post(
    val authorid: String = "",
    val category: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val location: Map<String, Any> = mapOf(),
    val numPeople: Int = 0,
    val price: Int = 0,
    val re_location: Boolean = false,
    val region: Map<String, String> = mapOf(),
    val title: String = "",
    val uploadedAt: Long = 0L,
    val voteOptions: List<Map<String, Any>> = emptyList()
)
