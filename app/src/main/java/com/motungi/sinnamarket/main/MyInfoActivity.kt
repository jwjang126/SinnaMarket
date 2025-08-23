package com.motungi.sinnamarket.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.auth.LoginActivity

class MyInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var btnDeleteAccount: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_info)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        loadUserInfo()

        btnDeleteAccount.setOnClickListener{
            showConfirmDialog()
        }
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

    //회원 탈퇴
    private fun showConfirmDialog(){
        AlertDialog.Builder(this)
            .setTitle("회원 탈퇴")
            .setMessage("정말로 탈퇴하시겠습니까?")
            .setPositiveButton("확인") {_, _ -> showPasswordDialog()}
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showPasswordDialog() {
        val input = EditText(this).apply{
            hint = "비밀번호 입력"
        }
        AlertDialog.Builder(this)
            .setTitle("비밀번호 확인")
            .setView(input)
            .setPositiveButton("확인"){_, _ ->
                val password = input.text.toString()
                if(password.isNotEmpty()){
                    reAuthenticateAndDelete(password)
                }
                else{
                    showError("비밀번호를 입력해주세요.")
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun reAuthenticateAndDelete(password: String){
        val user = auth.currentUser
        val email = user?.email

        if(user != null && email !=null){
            val credential = EmailAuthProvider.getCredential(email, password)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    val uid = user.uid
                    db.collection("users").document(uid).delete()
                        .addOnSuccessListener {
                            user.delete()
                                .addOnSuccessListener {
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                }
                                .addOnFailureListener{e->
                                    showError("계정 삭제 실패")
                                }
                        }
                        .addOnFailureListener{e->
                            showError("데이터 삭제 실패")
                        }
                }
                .addOnFailureListener{e->
                    showError("비밀번호가 올바르지 않습니다")
                }
        }
    else{
        showError("로그인된 사용자가 없습니다")
    }
}
    private fun showError(msg:String){
        AlertDialog.Builder(this)
            .setTitle("오류")
            .setMessage(msg)
            .setPositiveButton("확인", null)
            .show()
    }
}