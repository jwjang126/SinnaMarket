package com.motungi.sinnamarket.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.motungi.sinnamarket.R
import com.motungi.sinnamarket.auth.WriteActivity
import com.motungi.sinnamarket.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categories = listOf("가공식품(냉동)", "가공식품(비냉동)", "신선식품", "대량 물건")
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout

        viewPager.adapter = ViewPagerAdapter(this, categories)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = categories[position]
        }.attach()

        binding.bottomNavigationBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    viewPager.currentItem = 0
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
            showRegionMenu(it)
        }

        // 검색 아이콘 클릭 시 키보드 뜨도록 수정
        binding.searchIcon.setOnClickListener {
            // 키보드를 띄우는 로직
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            Toast.makeText(this, "키보드가 열립니다.", Toast.LENGTH_SHORT).show()
        }

        binding.fabWrite.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showMyInfo() {
        val myInfo = """
            이름: 김민준
            전화번호: 010-1234-5678
            이메일: minjun.kim@example.com
            닉네임: 알뜰한민준
            사는 지역: 대구 북구
        """.trimIndent()
        Toast.makeText(this, myInfo, Toast.LENGTH_LONG).show()
    }

    private fun showRegionMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menu.add(Menu.NONE, 0, 0, "수성구")
        popupMenu.menu.add(Menu.NONE, 1, 1, "동구")
        popupMenu.menu.add(Menu.NONE, 2, 2, "서구")
        popupMenu.setOnMenuItemClickListener { menuItem ->
            binding.regionText.text = menuItem.title
            Toast.makeText(this, "${menuItem.title}로 지역이 변경되었습니다.", Toast.LENGTH_SHORT).show()
            true
        }
        popupMenu.show()
    }
}