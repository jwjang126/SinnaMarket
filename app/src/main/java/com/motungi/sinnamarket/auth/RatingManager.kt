package com.motungi.sinnamarket.auth

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object RatingManager {
    private val db = FirebaseFirestore.getInstance()
    // 평가 정보 파베에 저장
    fun submitRatingToFirebase(ratedUid: String, raterUid: String, score: Float, reason: String, chatRoomId: String){

        val ratingDocRef = db.collection("user-ratings").document(ratedUid)
            .collection("ratings").document("${chatRoomId}_${raterUid}") // chatroomId와 raterUid 조합

        val ratingData = hashMapOf(
            "score" to score,
            "reason" to reason,
            "raterUid" to raterUid,
            "chatRoomId" to chatRoomId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        ratingDocRef.set(ratingData).addOnSuccessListener{
            updateUserAverageRating(ratedUid)
        }
    }

    // 평균 평점 계산, 업데이트
    private fun updateUserAverageRating(ratedUid: String){
        db.collection("user-ratings").document(ratedUid).collection("ratings").get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) return@addOnSuccessListener

                var totalScore = 0.0
                for (doc in documents) {
                    doc.getDouble("score")?.let {
                        totalScore += it
                    }
                }
                val averageRating = totalScore / documents.size()

                // 소수점 둘째자리에서 반올림해서 첫째자리까지만 저장
                val roundedRating = (Math.round(averageRating * 10) / 10.0)

                // users 컬렉션의 최종 평점 업데이트
                db.collection("users").document(ratedUid).update("rating", averageRating)
            }
    }
}