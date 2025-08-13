package com.motungi.sinnamarket.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.main.MainActivity

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 입력 필드
        val etName = findViewById<EditText>(R.id.etName)
        val etPhonenumber = findViewById<EditText>(R.id.etPhonenumber)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etNickname = findViewById<EditText>(R.id.etNickname)
        val spinnerRegion = findViewById<Spinner>(R.id.spinnerRegion)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        // 지역 스피너 설정
        val regions = listOf("지역을 선택하세요", "중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군", "군위군")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regions)
        spinnerRegion.adapter = adapter

        // 회원가입
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val phonenumber = etPhonenumber.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val nickname = etNickname.text.toString()
            val region = spinnerRegion.selectedItem.toString()

            if (name.isEmpty() || phonenumber.isEmpty() || email.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 닉네임 중복 검사
            db.collection("users")
                .whereEqualTo("nickname", nickname)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents != null && !documents.isEmpty) {
                        Toast.makeText(this, "이미 사용 중인 닉네임입니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        // 닉네임이 중복되지 않으면 회원가입 진행
                        createAccountAndSaveUser(name, phonenumber, email, password, nickname, region)
                    }
                }
                .addOnFailureListener { e ->
                    // 첫 가입 시도 시 컬렉션이 없어도 여기로 빠질 수 있음
                    // 그냥 닉네임 중복 없음으로 처리하고 진행
                    createAccountAndSaveUser(name, phonenumber, email, password, nickname, region)
                }
        }

        fun createAccountAndSaveUser(
            name: String,
            phonenumber: String,
            email: String,
            password: String,
            nickname: String,
            region: String
        ) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser!!.uid
                    val user = hashMapOf(
                        "name" to name,
                        "phonenumber" to phonenumber,
                        "email" to email,
                        "nickname" to nickname,
                        "region" to region,
                        "location" to hashMapOf("lat" to 0.0, "lng" to 0.0),
                        "rating" to 0.0,
                        "is_blocked" to false
                    )
                    db.collection("users").document(uid).set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "회원가입이 완료되었습니다", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {//DB 저장 실패
                            Toast.makeText(this, "오류: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {//회원가입 실패
                    Toast.makeText(this, "오류: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun createAccountAndSaveUser(
        name: String,
        phonenumber: String,
        email: String,
        password: String,
        nickname: String,
        region: String
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser!!.uid
                val user = hashMapOf(
                    "name" to name,
                    "phonenumber" to phonenumber,
                    "email" to email,
                    "nickname" to nickname,
                    "region" to region,
                    "location" to hashMapOf("lat" to 0.0, "lng" to 0.0),
                    "rating" to 0.0,
                    "is_blocked" to false
                )

                // Firestore에 저장
                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "회원가입이 완료되었습니다", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("SignUp", "오류: ${e.message}")//Firestore 저장 실패
                        Toast.makeText(this, "오류: ${e.message}", Toast.LENGTH_SHORT).show()//DB 저장 실패
                        // Auth 롤백
                        auth.currentUser?.delete()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("SignUp", "오류: ${e.message}")//회원가입 실패
                Toast.makeText(this, "오류: ${e.message}", Toast.LENGTH_SHORT).show()//회원가입 실패
            }
    }
}
