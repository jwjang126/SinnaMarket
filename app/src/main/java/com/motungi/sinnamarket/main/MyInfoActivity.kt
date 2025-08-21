package com.motungi.sinnamarket.main

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.motungi.sinnamarket.R

class MyInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_info)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserInfo()
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = currentUser.uid
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: "닉네임 없음"
                    val email = document.getString("email") ?: "이메일 없음"
                    val phonenumber = document.getString("phonenumber") ?: "전화번호 없음" // 추가된 부분
                    val rating = document.getDouble("rating") ?: 0.0 // 추가된 부분

                    val regionMap = document.get("region") as? Map<String, String>
                    val district = regionMap?.get("district") ?: "지역 정보 없음"
                    val dong = regionMap?.get("dong") ?: ""
                    val region = if (dong.isNotEmpty()) "$district $dong" else district

                    findViewById<TextView>(R.id.tvNickname).text = "닉네임: $nickname"
                    findViewById<TextView>(R.id.tvEmail).text = "이메일: $email"
                    findViewById<TextView>(R.id.tvPhonenumber).text = "전화번호: $phonenumber" // 추가된 부분
                    findViewById<TextView>(R.id.tvRegion).text = "지역: $region"
                    findViewById<TextView>(R.id.tvRating).text = "평점: $rating" // 추가된 부분
                } else {
                    Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyInfoActivity", "Error loading user info", e)
                Toast.makeText(this, "사용자 정보를 불러오는데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}