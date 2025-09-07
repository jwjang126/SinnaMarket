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
import androidx.appcompat.widget.SearchView
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

        binding.fabWrite.bringToFront()

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

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    val intent = Intent(this@MainActivity, SearchActivity::class.java)
                    intent.putExtra("search_query", query)
                    startActivity(intent)
                    hideSearchAndKeyboard()
                } else {
                    Toast.makeText(this@MainActivity, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        binding.bottomNavigationBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.viewPager.currentItem = 0
                    Toast.makeText(this, "홈 화면입니다.", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_chat -> {
                    val intent = Intent(this, ChatRoomListActivity::class.java)
                    startActivity(intent)
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
            if (binding.searchView.visibility == View.GONE) {
                binding.searchView.visibility = View.VISIBLE
                binding.searchView.requestFocus()
                showKeyboard()
            } else {
                hideSearchAndKeyboard()
            }
        }

        binding.fabWrite.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showMyInfo() {
        val intent = Intent(this, MyInfoActivity::class.java)
        startActivity(intent)
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

                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("selected_region", selectedDong).apply()

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

    private fun performSearch(query: String?) {
        if (query.isNullOrBlank()) {
            Toast.makeText(this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, SearchActivity::class.java)
        intent.putExtra("search_query", query)
        startActivity(intent)
    }

    private fun hideSearchAndKeyboard() {
        binding.searchView.visibility = View.GONE
        hideKeyboard()
        binding.searchView.setQuery("", false)
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchView, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
    }
}