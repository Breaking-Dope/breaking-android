package com.dope.breaking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.adapter.FinanceViewPagerAdapter
import com.dope.breaking.databinding.ActivityFinanceBinding
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.util.ValueUtil
import com.google.android.material.tabs.TabLayoutMediator
import java.text.DecimalFormat

class FinanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFinanceBinding
    private val decimalFormat = DecimalFormat("#,###") // 숫자 포맷팅

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinanceBinding.inflate(layoutInflater)

        initTabLayout() // 탭 설정

        // 툴바 뒤로가기 버튼 클릭 시
        binding.tbFinance.setNavigationOnClickListener {
            finish()
        }

        // 현재 보유 금액 텍스트 보여주기
        binding.tvAssessmentValue.text =
            decimalFormat.format(ResponseExistLogin.baseUserInfo!!.balance) + "원"

        setContentView(binding.root)
    }

    /**
     * 탭 레이아웃 초기 설정 함수
     * @author Seunggun Sin
     * @since 2022-09-02 | 2022-09-04
     */
    private fun initTabLayout() {
        binding.vpFinance.adapter = FinanceViewPagerAdapter(
            supportFragmentManager, lifecycle
        )
        binding.vpFinance.getChildAt(0).overScrollMode =
            RecyclerView.OVER_SCROLL_NEVER // viewpager overscroll 이펙트 제거

        TabLayoutMediator(binding.tlFinance, binding.vpFinance) { tab, position ->
            tab.text = ValueUtil.FINANCE_TAB_TEXT[position]
        }.attach()
    }
}