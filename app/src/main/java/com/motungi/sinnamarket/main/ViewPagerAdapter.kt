package com.motungi.sinnamarket.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.util.SparseArray
import android.view.ViewGroup

class ViewPagerAdapter(fragmentActivity: FragmentActivity, private val categories: List<String>, private var region: String) :
    FragmentStateAdapter(fragmentActivity) {

    // Fragment 인스턴스를 저장할 SparseArray
    private val fragments = SparseArray<Fragment>()

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        val fragment = PostListFragment.newInstance(categories[position], region)
        fragments.put(position, fragment)
        return fragment
    }

    // 이전에 제안했던 updateFragments() 함수를 호출하는 코드를 제거했습니다.
    // 대신 MainActivity에서 ViewPagerAdapter의 updateRegion()을 직접 호출합니다.

    // MainActivity에서 호출될 함수
    fun updateRegion(newRegion: String) {
        if (this.region != newRegion) {
            this.region = newRegion
            // 모든 Fragment의 지역 정보를 업데이트
            for (i in 0 until fragments.size()) {
                val fragment = fragments.valueAt(i)
                (fragment as? PostListFragment)?.updateRegion(newRegion)
            }
        }
    }

    // 현재 포지션의 프래그먼트를 가져오는 함수 추가
    fun getCurrentFragment(position: Int): Fragment? {
        return fragments.get(position)
    }
}