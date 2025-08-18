package com.motungi.sinnamarket.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity, private val categories: List<String>, private var region: String) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = categories.size

    fun updateRegion(newRegion: String) {
        this.region = newRegion
        notifyDataSetChanged()
    }

    override fun createFragment(position: Int): Fragment {
        return PostListFragment.newInstance(categories[position], region)
    }
}
