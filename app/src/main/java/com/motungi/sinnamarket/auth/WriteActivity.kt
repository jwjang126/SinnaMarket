package com.motungi.sinnamarket.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.main.MainActivity
import com.motungi.sinnamarket.main.MapSelectActivity
import java.util.UUID

class WriteActivity : AppCompatActivity() {

    private val categories = arrayOf("신선식품", "가공식품(비냉동)","가공식품(냉동)", "식품 이외")
    private val numOptions = (1..15).map { "${it}명" }
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

    private val imageUris = mutableListOf<Uri>()
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var selectPhotoBtn: Button

    private val ampm = arrayOf("선택하세요","오전","오후")
    private val hour = (1..12).map { "${it}시" }.toMutableList().apply { add(0, "선택하세요") }
    private val min = (0..59).map { "${it}분" }.toMutableList().apply { add(0, "선택하세요") }
    private val year = (2025..2030).map { "${it}년" }
    private val month = (1..12).map { "${it}월" }
    private val day = (1..31).map { "${it}일" }
    private var isUploading = false // 업로드 중인지 체크하는 플래그
    private lateinit var submitBtn: Button


    // 여러 장 이미지 선택
    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            imageUris.clear()
            imageUris.addAll(uris)
            photoAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        photoRecyclerView = findViewById(R.id.photoRecyclerView)
        photoAdapter = PhotoAdapter(imageUris)
        photoRecyclerView.adapter = photoAdapter
        photoRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        selectPhotoBtn = findViewById(R.id.selectPhotoBtn)
        selectPhotoBtn.setOnClickListener { pickImagesLauncher.launch("image/*") }

        val itemNameInput = findViewById<EditText>(R.id.itemNameInput)
        val itemPriceInput = findViewById<EditText>(R.id.itemPriceInput)
        val itemDescInput = findViewById<EditText>(R.id.itemDescInput)

        val numSpinner = findViewById<Spinner>(R.id.numSpinner)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val districtSpinner = findViewById<Spinner>(R.id.districtSpinner)
        val dongSpinner = findViewById<Spinner>(R.id.dongSpinner)
        val yearSpinner = findViewById<Spinner>(R.id.yearSpinner)
        val monthSpinner = findViewById<Spinner>(R.id.monthSpinner)
        val daySpinner = findViewById<Spinner>(R.id.daySpinner)

        val option1_ap = findViewById<Spinner>(R.id.option1_ap)
        val option2_ap = findViewById<Spinner>(R.id.option2_ap)
        val option3_ap = findViewById<Spinner>(R.id.option3_ap)
        val option4_ap = findViewById<Spinner>(R.id.option4_ap)
        val option5_ap = findViewById<Spinner>(R.id.option5_ap)

        val option1_hour = findViewById<Spinner>(R.id.option1_hour)
        val option2_hour = findViewById<Spinner>(R.id.option2_hour)
        val option3_hour = findViewById<Spinner>(R.id.option3_hour)
        val option4_hour = findViewById<Spinner>(R.id.option4_hour)
        val option5_hour = findViewById<Spinner>(R.id.option5_hour)

        val option1_min = findViewById<Spinner>(R.id.option1_min)
        val option2_min = findViewById<Spinner>(R.id.option2_min)
        val option3_min = findViewById<Spinner>(R.id.option3_min)
        val option4_min = findViewById<Spinner>(R.id.option4_min)
        val option5_min = findViewById<Spinner>(R.id.option5_min)

        val apSpinners = arrayOf(option1_ap, option2_ap, option3_ap, option4_ap, option5_ap)
        val hourSpinners = arrayOf(option1_hour, option2_hour, option3_hour, option4_hour, option5_hour)
        val minSpinners = arrayOf(option1_min, option2_min, option3_min, option4_min, option5_min)

        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        districtSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districtMap.keys.toList())
        numSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, numOptions)
        yearSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, year)
        monthSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, month)
        daySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, day)
        for (spinner in apSpinners) spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ampm)
        for (spinner in hourSpinners) spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hour)
        for (spinner in minSpinners) spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, min)

        districtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedDistrict = parent?.getItemAtPosition(position).toString()
                val dongList = districtMap[selectedDistrict] ?: listOf("선택하세요")
                dongSpinner.adapter = ArrayAdapter(this@WriteActivity, android.R.layout.simple_spinner_dropdown_item, dongList)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val submitBtn = findViewById<Button>(R.id.submitBtn)
        val checkBoxOption = findViewById<CheckBox>(R.id.checkbox)

        submitBtn.setOnClickListener {
            if (isUploading) {
                Toast.makeText(this, "업로드가 진행 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isUploading = true // 업로드 시작
            submitBtn.isEnabled = false // 버튼 비활성화

            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->

                    val voteOptions = mutableListOf<Map<String, Any>>()
                    for (i in 0 until 5) {
                        val ap = apSpinners[i].selectedItem.toString()
                        val hr = hourSpinners[i].selectedItem.toString()
                        val mn = minSpinners[i].selectedItem.toString()

                        if (ap != "선택하세요" && hr != "선택하세요" && mn != "선택하세요") {
                            val timeStr = "$ap$hr$mn"
                            voteOptions.add(mapOf("time" to timeStr, "voters" to emptyList<String>()))
                        }
                    }

                    saveItemToFirestore(0.0, 0.0, checkBoxOption.isChecked, voteOptions)
                }
        }

        val openMapBtn = findViewById<Button>(R.id.openMap)
        openMapBtn.setOnClickListener {
            val intent = Intent(this, MapSelectActivity::class.java)
            startActivityForResult(intent, 1001) // 1001은 요청 코드
        }

    }

    private fun saveItemToFirestore(lat: Double, lng: Double, isChecked: Boolean, voteOptions: List<Map<String, Any>>) {
        val itemName = findViewById<EditText>(R.id.itemNameInput).text.toString()
        val itemDesc = findViewById<EditText>(R.id.itemDescInput).text.toString()
        val itemPrice = findViewById<EditText>(R.id.itemPriceInput).text.toString().toIntOrNull() ?: 0
        val category = findViewById<Spinner>(R.id.categorySpinner).selectedItem.toString()
        val numPeople = findViewById<Spinner>(R.id.numSpinner).selectedItem.toString().replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
        val district = findViewById<Spinner>(R.id.districtSpinner).selectedItem.toString()
        val dong = findViewById<Spinner>(R.id.dongSpinner).selectedItem.toString()

        val db = FirebaseFirestore.getInstance()
        val storageRef = FirebaseStorage.getInstance().reference
        val imageUrls = mutableListOf<String>()

        val data = hashMapOf(
            "title" to itemName,
            "description" to itemDesc,
            "price" to itemPrice,
            "category" to category,
            "numPeople" to numPeople,
            "re_location" to isChecked,
            "region" to hashMapOf("district" to district, "dong" to dong),
            "location" to hashMapOf("lat" to lat, "lng" to lng),
            "imageUrls" to imageUrls,
            "voteOptions" to voteOptions,
            "uploadedAt" to System.currentTimeMillis()
        )

        fun uploadImages(index: Int = 0, docRefId: String? = null) {
            if (index >= imageUris.size) {
                if (docRefId == null) {
                    db.collection("product").add(data).addOnSuccessListener { docRef ->
                        uploadImages(0, docRef.id)
                    }
                } else {
                    db.collection("product").document(docRefId).update("imageUrls", imageUrls).addOnCompleteListener{
                        Toast.makeText(this, "상품 등록 완료", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // WriteActivity 종료
                        isUploading = false // 업로드 완료 후 플래그 해제
                        submitBtn.isEnabled = true // 버튼 다시 활성화

                    }

                }
                return
            }

            val uri = imageUris[index]
            val fileName = "images/${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child(fileName)

            imageRef.putFile(uri).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    imageUrls.add(downloadUri.toString())
                    uploadImages(index + 1, docRefId)
                }
            }.addOnFailureListener {
                Log.e("WriteActivity", "이미지 업로드 실패: ${it.message}")
                uploadImages(index + 1, docRefId)
            }
        }

        uploadImages()
    }

}
