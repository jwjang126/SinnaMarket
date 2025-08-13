package com.motungi.sinnamarket.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
        val spinnerDistrict = findViewById<Spinner>(R.id.spinnerDistrict)
        val spinnerDong = findViewById<Spinner>(R.id.spinnerDong)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        // 1. 구 리스트
        val districtList = listOf("선택하세요", "중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군", "군위군")
        val districtAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districtList)
        spinnerDistrict.adapter = districtAdapter

// 2. 구-동 매핑
        val dongMap = mapOf(
            "중구" to listOf("선택하세요", "동인동", "삼덕동", "성내동", "대신동", "남산동", "대봉동"),
            "동구" to listOf("선택하세요", "신암동", "신천동", "효목동", "도평동", "불로봉무동", "지저동", "동촌동", "방촌동", "해안동", "안심동", "혁신동", "공산동"),
            "서구" to listOf("선택하세요", "내당동", "비산동", "평리동", "상중이동", "원대동"),
            "남구" to listOf("선택하세요", "이천동", "봉덕동", "대명동"),
            "북구" to listOf("선택하세요", "고성동", "칠성동", "침산동", "노원동", "산격동", "복현동", "대현동", "검단동", "무태조야동", "관문동", "태전동", "구암동", "관음동", "읍내동", "동촌동", "국우동"),
            "수성구" to listOf("선택하세요", "범어동", "만촌동", "수성동", "황금동", "중동", "상동", "파동", "두산동", "지산동", "범물동", "고산동"),
            "달서구" to listOf("선택하세요", "성당동", "두류동", "본리동", "감삼동", "죽전동", "장기동", "용산동", "이곡동", "신당동", "월성동", "진천동", "유천동", "상인동", "도원동", "송현동", "본동"),
            "달성군" to listOf("선택하세요", "화원읍", "논공읍", "다사읍", "유가읍", "옥포읍", "현풍읍", "가창면", "하빈면", "구지면"),
            "군위군" to listOf("선택하세요", "군위읍", "소보면", "효령면", "부계면", "우보면", "의흥면", "산성면", "삼국유사면")
        )

// 3. 구 선택 시 해당 동 리스트 적용
        spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDistrict = districtList[position]
                val dongList = dongMap[selectedDistrict] ?: listOf("선택하세요")
                val dongAdapter = ArrayAdapter(this@SignUpActivity, android.R.layout.simple_spinner_dropdown_item, dongList)
                spinnerDong.adapter = dongAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 회원가입
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val phonenumber = etPhonenumber.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val nickname = etNickname.text.toString()
            val district = spinnerDistrict.selectedItem.toString()
            val dong = spinnerDong.selectedItem.toString()
            val region = mapOf(
                "district" to district,
                "dong" to dong
            )

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
    }

    private fun createAccountAndSaveUser(
        name: String,
        phonenumber: String,
        email: String,
        password: String,
        nickname: String,
        region: Map<String, String>
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
