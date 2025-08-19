package com.motungi.sinnamarket.auth

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.auth.PhotoAdapter
import com.motungi.sinnamarket.main.MainActivity
import com.motungi.sinnamarket.main.MapSelectActivity
import java.util.UUID

class WriteActivity : AppCompatActivity() {

    private val imageUris = mutableListOf<Uri>()
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var selectPhotoBtn: Button
    private lateinit var submitBtn: Button

    private var isUploading = false

    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0
    private var selectedLocationDesc: String = ""

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            imageUris.clear()
            imageUris.addAll(uris)
            photoAdapter.notifyDataSetChanged()
        }
    }

    private val pickMapLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            selectedLat = data?.getDoubleExtra("selectedLat", 0.0) ?: 0.0
            selectedLng = data?.getDoubleExtra("selectedLng", 0.0) ?: 0.0
            // 위치 설명 입력
            val editText = EditText(this).apply { hint = "예: 경북대학교 IT1호관 앞" }
            AlertDialog.Builder(this)
                .setTitle("위치 설명 입력")
                .setMessage("선택한 위치를 설명해주세요")
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
        submitBtn.setOnClickListener { submitItem() }

        val openMapBtn = findViewById<Button>(R.id.openMap)
        openMapBtn.setOnClickListener {
            val intent = Intent(this, MapSelectActivity::class.java)
            pickMapLauncher.launch(intent)
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

        saveItemToFirestore(authorid = currentUser.uid)
    }

    private fun saveItemToFirestore(authorid: String) {
        val itemName = findViewById<EditText>(R.id.itemNameInput).text.toString()
        val itemDesc = findViewById<EditText>(R.id.itemDescInput).text.toString()
        val itemPrice = findViewById<EditText>(R.id.itemPriceInput).text.toString().toIntOrNull() ?: 0
        val category = findViewById<Spinner>(R.id.categorySpinner).selectedItem.toString()
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
            "authorid" to authorid,
            "region" to hashMapOf("district" to district, "dong" to dong),
            "location" to hashMapOf("lat" to selectedLat, "lng" to selectedLng, "desc" to selectedLocationDesc),
            "imageUrls" to imageUrls,
            "uploadedAt" to System.currentTimeMillis()
        )

        db.collection("product").add(data)
            .addOnSuccessListener { docRef ->
                uploadImages(0, docRef.id, imageUrls, storageRef)
            }
            .addOnFailureListener {
                Toast.makeText(this, "상품 등록 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                isUploading = false
                submitBtn.isEnabled = true
            }
    }

    private fun uploadImages(index: Int, docId: String, imageUrls: MutableList<String>, storageRef: StorageReference) {
        if (index >= imageUris.size) {
            // 모든 이미지 업로드 완료
            FirebaseFirestore.getInstance().collection("product").document(docId)
                .update("imageUrls", imageUrls)
                .addOnCompleteListener {
                    Toast.makeText(this, "상품 등록 완료", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    isUploading = false
                    submitBtn.isEnabled = true
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
                    uploadImages(index + 1, docId, imageUrls, storageRef)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                isUploading = false
                submitBtn.isEnabled = true
            }
    }
}
