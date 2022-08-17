package com.dope.breaking.board

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dope.breaking.R
import com.dope.breaking.databinding.ActivityPostDetailBinding

class PostDetailActivity : AppCompatActivity() {

    private var mbinding : ActivityPostDetailBinding? = null
    private val binding get() = mbinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_post_detail)
    }

    /**
     * @description - 게시글 상세 페이지 상단 툴 바에 대한 설정 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-15
     */
    private fun settingPostToolBar() {
        setSupportActionBar(binding.postDetailPageToolBar) // 툴 바 설정
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) // 왼쪽 상단 버튼 만들기
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24) // 왼쪽 상단 아이콘
        supportActionBar!!.setDisplayShowTitleEnabled(true) // 툴 바에 타이틀 보이게
    }
}