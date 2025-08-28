package com.motungi.sinnamarket.auth

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ServerValue

object RatingManager {
    // 평가 정보 파베에 저장
    fun submitRatingToFirebase(ratedUid: String, raterUid: String, score: Float, reason: String){
        val database = Firebase.database.reference

        val newRatingRef = database.child("user-ratings").child(ratedUid).push()

        val ratingData = hashMapOf(
            "score" to score,
            "reason" to reason,
            "raterUid" to raterUid,
            "timestamp" to ServerValue.TIMESTAMP
        )

        newRatingRef.setValue(ratingData).addOnSuccessListener{
            updateUserAverageRating(ratedUid)
        }
    }

    // 평균 평점 계산, 업데이트
    private fun updateUserAverageRating(ratedUid: String){
        val database = Firebase.database.reference
        val ratingsRef = database.child("user-ratings").child(ratedUid)
        val userRatingRef = database.child("users").child(ratedUid).child("rating")

        ratingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                var totalScore = 0.0
                val ratingCount = snapshot.childrenCount

                if(ratingCount==0L) return

                for (ratingSnapshot in snapshot.children){
                    val score = ratingSnapshot.child("score").getValue(Double::class.java)
                    score?.let { totalScore += it }
                }

                val averageRating = totalScore / ratingCount
                userRatingRef.setValue(averageRating)
            }

            override fun onCancelled(error: DatabaseError){
                Log.e("RatingManger", error.message)
            }
        })
    }
}