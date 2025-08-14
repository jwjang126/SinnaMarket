package com.motungi.sinnamarket.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.motungi.sinnamarket.R
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private val categories = arrayOf("신선식품", "가공식품", "식품 이외")

    private val districtMap = mapOf(
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

    private val ampm = arrayOf("오전", "오후")
    private val hours = (1..12).map { it.toString() }.toTypedArray()
    private val mins = (0..59).map { it.toString() }.toTypedArray()
    private val PICK_IMAGE_REQUEST = 100
    private var imageUri: Uri? = null
    private lateinit var photoPreview: ImageView
    private lateinit var selectPhotoBtn: Button

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        val itemNameInput = findViewById<EditText>(R.id.itemNameInput)
        val itemPriceInput = findViewById<EditText>(R.id.itemPriceInput)
        val itemDescInput = findViewById<EditText>(R.id.itemDescInput)

        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val districtSpinner = findViewById<Spinner>(R.id.districtSpinner)
        val dongSpinner = findViewById<Spinner>(R.id.dongSpinner)

        // --- 스피너 어댑터 연결 ---
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        districtSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districtMap.keys.toList())

        // 구 선택 시 동 스피너 업데이트
        districtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDistrict = parent?.getItemAtPosition(position).toString()
                val dongList = districtMap[selectedDistrict] ?: listOf("선택하세요")
                dongSpinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, dongList)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        //사진 첨부
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        photoPreview = findViewById(R.id.photoPreview)
        selectPhotoBtn = findViewById(R.id.selectPhotoBtn)

        selectPhotoBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            photoPreview.setImageURI(imageUri)
            uploadImageToFirebase()
        }
    }

    private fun uploadImageToFirebase() {
        if (imageUri == null) return

        val storageRef = FirebaseStorage.getInstance().reference
        val fileName = "images/${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(fileName)

        imageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    // 여기서 Firestore/Realtime DB에 imageUrl 저장
                    println("Firebase Image URL: $imageUrl")
                }
            }
            .addOnFailureListener {
                println("Upload failed: ${it.message}")
            }
    }
}
