package com.motungi.sinnamarket.auth

import android.app.Activity
import android.app.AlertDialog
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
import com.motungi.sinnamarket.auth.PhotoAdapter
import com.motungi.sinnamarket.main.MainActivity
import com.motungi.sinnamarket.main.MapSelectActivity
import java.util.*

class WriteActivity : AppCompatActivity() {

    private val categories = arrayOf("신선식품", "가공식품(비냉동)", "가공식품(냉동)", "식품 이외")
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

    private val ampm = arrayOf("선택하세요", "오전", "오후")
    private val hour = (1..12).map { "${it}시" }.toMutableList().apply { add(0, "선택하세요") }
    private val min = (0..59).map { "${it}분" }.toMutableList().apply { add(0, "선택하세요") }
    private val year = (2025..2030).map { "${it}년" }
    private val month = (1..12).map { "${it}월" }
    private val day = (1..31).map { "${it}일" }
    private var isUploading = false

    private lateinit var submitBtn: Button
    private lateinit var checkBoxOption: CheckBox

    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0
    private var selectedLocationDesc: String = ""

    private lateinit var apSpinners: Array<Spinner>
    private lateinit var hourSpinners: Array<Spinner>
    private lateinit var minSpinners: Array<Spinner>

    // --- 이미지 선택 런처 ---
    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                imageUris.clear()
                imageUris.addAll(uris)
                photoAdapter.notifyDataSetChanged()
            }
        }

    private val pickMapLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                selectedLat = data?.getDoubleExtra("selectedLat", 0.0) ?: 0.0
                selectedLng = data?.getDoubleExtra("selectedLng", 0.0) ?: 0.0

                val editText = EditText(this).apply { hint = "예: 경북대학교 IT1호관 앞" }
                AlertDialog.Builder(this)
                    .setTitle("위치 설명 입력")
                    .setView(editText)
                    .setPositiveButton("확인") { dialog, _ ->
                        selectedLocationDesc = editText.text.toString()
                        Toast.makeText(this, "위치가 설정되었습니다: $selectedLocationDesc", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
                    .show()
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

        submitBtn = findViewById(R.id.submitBtn)
        checkBoxOption = findViewById(R.id.checkbox)
        submitBtn.setOnClickListener { submitItem() }

        // --- 스피너 초기화 ---
        apSpinners = arrayOf(
            findViewById(R.id.option1_ap),
            findViewById(R.id.option2_ap),
            findViewById(R.id.option3_ap),
            findViewById(R.id.option4_ap),
            findViewById(R.id.option5_ap)
        )
        hourSpinners = arrayOf(
            findViewById(R.id.option1_hour),
            findViewById(R.id.option2_hour),
            findViewById(R.id.option3_hour),
            findViewById(R.id.option4_hour),
            findViewById(R.id.option5_hour)
        )
        minSpinners = arrayOf(
            findViewById(R.id.option1_min),
            findViewById(R.id.option2_min),
            findViewById(R.id.option3_min),
            findViewById(R.id.option4_min),
            findViewById(R.id.option5_min)
        )

        // --- 스피너 어댑터 ---
        val numSpinner = findViewById<Spinner>(R.id.numSpinner)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val districtSpinner = findViewById<Spinner>(R.id.districtSpinner)
        val dongSpinner = findViewById<Spinner>(R.id.dongSpinner)
        val yearSpinner = findViewById<Spinner>(R.id.yearSpinner)
        val monthSpinner = findViewById<Spinner>(R.id.monthSpinner)
        val daySpinner = findViewById<Spinner>(R.id.daySpinner)

        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        districtSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districtMap.keys.toList())
        numSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, numOptions)
        yearSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, year)
        monthSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, month)
        daySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, day)

        for (spinner in apSpinners) spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ampm)
        for (spinner in hourSpinners) spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hour)
        for (spinner in minSpinners) spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, min)

        // --- 지역 선택 ---
        districtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedDistrict = parent?.getItemAtPosition(position).toString()
                val dongList = districtMap[selectedDistrict] ?: listOf("선택하세요")
                dongSpinner.adapter = ArrayAdapter(this@WriteActivity, android.R.layout.simple_spinner_dropdown_item, dongList)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.openMap).setOnClickListener {
            pickMapLauncher.launch(Intent(this, MapSelectActivity::class.java))
        }
    }

    private fun submitItem() {
        if (isUploading) {
            Toast.makeText(this, "업로드 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        submitBtn.isEnabled = false

        val authorid = currentUser.uid
        val voteOptions = mutableListOf<Map<String, Any>>()

        for (i in 0 until 5) {
            val ap = apSpinners[i].selectedItem.toString()
            val hr = hourSpinners[i].selectedItem.toString()
            val mn = minSpinners[i].selectedItem.toString()
            if (ap != "선택하세요" && hr != "선택하세요" && mn != "선택하세요") {
                voteOptions.add(mapOf("time" to "$ap$hr$mn", "voters" to emptyList<String>()))
            }
        }

        saveItemToFirestore(
            lat = selectedLat,
            lng = selectedLng,
            isChecked = checkBoxOption.isChecked,
            voteOptions = voteOptions,
            authorid = authorid
        )
    }

    private fun saveItemToFirestore(lat: Double, lng: Double, isChecked: Boolean, voteOptions: List<Map<String, Any>>, authorid: String) {
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
            "authorid" to authorid,
            "re_location" to isChecked,
            "region" to hashMapOf("district" to district, "dong" to dong),
            "location" to hashMapOf("lat" to lat, "lng" to lng, "desc" to selectedLocationDesc),
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
                    db.collection("product").document(docRefId)
                        .update("imageUrls", imageUrls)
                        .addOnCompleteListener {
                            Toast.makeText(this, "상품 등록 완료", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                            isUploading = false
                            submitBtn.isEnabled = true
                        }
                }
                return
            }

            val uri = imageUris[index]
            val fileName = "images/${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child(fileName)

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrls.add(downloadUri.toString())
                        uploadImages(index + 1, docRefId)
                    }
                }
                .addOnFailureListener {
                    Log.e("WriteActivity", "이미지 업로드 실패: ${it.message}")
                    uploadImages(index + 1, docRefId)
                }
        }

        uploadImages()
    }
}
