package com.dope.breaking.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dope.breaking.fragment.user_tab.BookmarkedTabFragment
import com.dope.breaking.fragment.user_tab.PostTabFragment
import com.dope.breaking.fragment.user_tab.PurchasedTabFragment

private const val NUM_TABS = 3 // 탭의 개수

/**
 * 유저 페이지의 게시글 리스트를 보여주는 ViewPager 의 어댑터
 */
class UserViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val userId: Long
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putLong("userId", userId)
        when (position) {
            0 -> {
                val fragment = PostTabFragment()
                fragment.arguments = bundle
                return fragment
            } // 0번째 인덱스일 때 "작성 제보 Fragment"}
            1 -> {
                val fragment = PurchasedTabFragment()
                fragment.arguments = bundle
                return fragment
            } // 1번째 인덱스일 때 "구매 제보 Fragment"
            2 -> {
                val fragment = BookmarkedTabFragment()
                fragment.arguments = bundle
                return fragment
            } // 2번째 인덱스일 때 "북마크 제보 Fragment"
        }
        return PostTabFragment() // default 로 "작성 제보 Fragment"
    }

}