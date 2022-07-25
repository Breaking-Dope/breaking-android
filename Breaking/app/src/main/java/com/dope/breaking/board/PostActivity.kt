package com.dope.breaking.board

import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dope.breaking.R
import com.dope.breaking.databinding.ActivityPostBinding


class PostActivity : AppCompatActivity() {

    private val TAG = "PostActivity.kt" // Tag Log

    private var mbinding : ActivityPostBinding? = null

    private val binding get() = mbinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingPostToolBar()  // 툴 바 설정
        allowScrollEditText() // EditText 스크롤 터치 이벤트 허용
    }

    /**
    @description - activity_post.xml에서 이미 최상위에 NestedScrollView가 정의되어 있기 때문에 EditText에 스크롤 옵션을 주어도 이벤트가 막히는 현상이 발생한다.
                   따라서 해당 메소드를 통해 EditText 가 터치되어있을 때 부모의 스크롤 권한을 가로채고, EditText가 아닌 바깥을 터치한다면 다시 부모 스크롤 뷰가 동작하도록
                   하는 메소드.
    @param - None
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-25
     */
    private fun allowScrollEditText(){
        // EditText 스크롤 터지 이벤트 리스너
        binding.etContent.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (v!!.id === R.id.et_content) { // 안쪽 EditText 를 클릭했다면
                    v!!.parent.requestDisallowInterceptTouchEvent(true) // 부모 뷰의 스크롤 이벤트 비허용
                    when (event!!.action and MotionEvent.ACTION_MASK) { // 만약 바깥 이벤트가 클릭되었다면
                        MotionEvent.ACTION_UP -> v!!.parent.requestDisallowInterceptTouchEvent(false) // 부모 뷰의 스크롤 이벤트 허용
                    }
                }
                return false
            }
        })
    }

    /**
    @description - 제보하기 페이지 상단 툴 바에 대한 설정 메소드
    @param - None
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-25
     */
    private fun settingPostToolBar(){
        setSupportActionBar(binding.postPageToolBar) // 툴 바 설정
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) // 왼쪽 상단 버튼 만들기
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24) // 왼쪽 상단 아이콘
        supportActionBar!!.setDisplayShowTitleEnabled(true) // 툴 바에 타이틀 보이게
    }

    // 툴 바의 item 선택 이벤트 리스너
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> { // 툴 바의 뒤로가기 키가 눌렸을 때 동작
                finish()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}