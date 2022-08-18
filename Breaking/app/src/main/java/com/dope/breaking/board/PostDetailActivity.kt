package com.dope.breaking.board

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.dope.breaking.R
import com.dope.breaking.databinding.ActivityPostDetailBinding

class PostDetailActivity : AppCompatActivity() {
    private val TAG = "PostDetailActivity.kt"
    private var mbinding : ActivityPostDetailBinding? = null
    private val binding get() = mbinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingPostToolBar()

        var getPostId = intent.getIntExtra("postId",-1)
        Log.d(TAG,"받아온 postId 값 : ${getPostId.toString()}")
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

    // 툴 바의 item 선택 이벤트 리스너
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { // 툴 바의 뒤로가기 키가 눌렸을 때 동작
                finish()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}