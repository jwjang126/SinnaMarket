package com.motungi.sinnamarket.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.auth.LoginActivity
import com.motungi.sinnamarket.auth.WriteActivity
import com.motungi.sinnamarket.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var selectedRegion: String = "수성구"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SharedPreferences에서 마지막으로 설정된 지역 정보를 불러옵니다.
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        selectedRegion = prefs.getString("selected_region", "수성구") ?: "수성구"
        binding.regionText.text = selectedRegion

        val categories = listOf("가공식품(냉동)", "가공식품(비냉동)", "신선식품", "식품 이외")
        viewPagerAdapter = ViewPagerAdapter(this, categories, selectedRegion)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.offscreenPageLimit = 1

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = categories[position]
        }.attach()

        binding.bottomNavigationBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.viewPager.currentItem = 0
                    Toast.makeText(this, "홈 화면입니다.", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_chat -> {
                    Toast.makeText(this, "채팅방 목록 화면으로 이동", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_my_info -> {
                    showMyInfo()
                    true
                }
                else -> false
            }
        }

        binding.regionText.setOnClickListener {
            showRegionDialog()
        }

        binding.searchIcon.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            Toast.makeText(this, "키보드가 열립니다.", Toast.LENGTH_SHORT).show()
        }

        binding.fabWrite.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            startActivity(intent)
        }

        binding.logoutIcon.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            }
            startActivity(intent)
            finish()
        }
    }

    private fun showMyInfo() {
        // 이 부분은 Firebase에서 실제 사용자 정보를 불러와야 합니다.
        val myInfo = """
            이름: 김민준
            전화번호: 010-1234-5678
            이메일: minjun.kim@example.com
            닉네임: 알뜰한민준
            사는 지역: 대구 북구
        """.trimIndent()
        Toast.makeText(this, myInfo, Toast.LENGTH_LONG).show()
    }

    private fun showRegionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_region_selection, null)
        val districtSpinner: Spinner = dialogView.findViewById(R.id.districtSpinner)
        val dongSpinner: Spinner = dialogView.findViewById(R.id.dongSpinner)

        val districts = districtMap.keys.toList()
        districtSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districts)

        districtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDistrict = parent?.getItemAtPosition(position).toString()
                val dongList = districtMap[selectedDistrict] ?: listOf("선택하세요")
                dongSpinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, dongList)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("거래 지역 선택")
            .setView(dialogView)
            .setPositiveButton("선택") { dialog, _ ->
                val selectedDong = dongSpinner.selectedItem.toString()
                if (selectedDong != "선택하세요" && selectedDong != selectedRegion) {
                    binding.regionText.text = selectedDong
                    selectedRegion = selectedDong

                    // SharedPreferences에 지역 정보 저장
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("selected_region", selectedDong).apply()

                    // ViewPagerAdapter를 통해 모든 Fragment 업데이트
                    viewPagerAdapter.updateRegion(selectedDong)

                    Toast.makeText(this, "$selectedDong 로 지역이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "동을 선택하거나, 기존 지역과 다른 지역을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}