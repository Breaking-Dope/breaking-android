package com.dope.breaking.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dope.breaking.fragment.user_tab.BookmarkedTabFragment
import com.dope.breaking.fragment.user_tab.PostTabFragment
import com.dope.breaking.fragment.user_tab.PurchasedTabFragment
import com.dope.breaking.model.response.ResponseExistLogin

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
        return if (ResponseExistLogin.baseUserInfo != null && ResponseExistLogin.baseUserInfo!!.userId == userId) NUM_TABS else 1
    }

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putLong("userId", userId)
        // 현재 유저가 본인이라면 모든 탭 다 보여주기
        if (ResponseExistLogin.baseUserInfo != null && ResponseExistLogin.baseUserInfo!!.userId == userId) {
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
        }
        // 다른 유저의 경우 작성한 제보 탭만 보여주기
        val fragment = PostTabFragment()
        fragment.arguments = bundle
        return fragment // default 로 "작성 제보 Fragment"
    }

}