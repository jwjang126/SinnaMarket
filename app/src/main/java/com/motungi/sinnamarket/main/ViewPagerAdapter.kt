package com.motungi.sinnamarket.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

// ViewPager 어댑터
class ViewPagerAdapter(fragmentActivity: FragmentActivity, private val categories: List<String>) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return PlaceholderFragment.newInstance(categories[position])
    }
}
