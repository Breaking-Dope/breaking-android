package com.dope.breaking.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dope.breaking.fragment.finance_tab.TransactionListTabFragment
import com.dope.breaking.fragment.finance_tab.ChargeTabFragment
import com.dope.breaking.fragment.finance_tab.WithdrawTabFragment

private const val NUM_TABS = 3

class FinanceViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ChargeTabFragment()
            1 -> WithdrawTabFragment()
            2 -> TransactionListTabFragment()
            else -> ChargeTabFragment()
        }
    }
}