package com.dope.breaking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.adapter.FollowAdapter
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.follow.Follow
import com.dope.breaking.model.FollowData
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FollowActivity : AppCompatActivity() {
    private val data = mutableListOf<FollowData>() // 팔로우 목록 저장하는 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
            ※ 팔로우, 팔로워 구분
            - 팔로우(팔로잉) 페이지 = true
            - 팔로워 페이지 = false
         */
        val state = intent.getBooleanExtra("state", true)
        val userId = intent.getLongExtra("userId", 0) // 리스트를 보여주고자 하는 대상의 고유 id
        val follow = Follow() // 팔로우 관련 기능 객체 생성

        CoroutineScope(Dispatchers.Main).launch {
            val requestToken =
                ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(this@FollowActivity).getTokenFromLocal()

            try {
                if (state) { // 팔로우 페이지라면
                    val list = follow.startGetFollowingList(requestToken, userId) // 팔로잉 리스트 요청
                    val size = list.size // 리스트 크기
                    showFollowList(size, state, list) // 가져온 리스트를 화면에 보여주기
                } else { // 팔로워 페이지라면
                    val list = follow.startGetFollowerList(requestToken, userId) // 팔로워 리스트 요청
                    val size = list.size // 리스트 크기
                    showFollowList(size, state, list) // 가져온 리스트를 화면에 보여주기
                }
            } catch (e: ResponseErrorException) {
                DialogUtil().SingleDialog(
                    this@FollowActivity,
                    "요청에 문제가 발생하였습니다.",
                    "확인"
                ).show()
            } catch (e: Exception) {
                DialogUtil().SingleDialog(
                    this@FollowActivity,
                    "예기치 못한 문제가 발생하였습니다.",
                    "확인"
                ).show()
            }
        }
    }

    /**
     * 목록이 비어있을 때에 대한 화면 처리
     * @param state(Boolean): 팔로우 리스트(true)인지 팔로워 리스트(false)인지 구분
     * @author Seunggun Sin
     * @since 2022-07-28
     */
    private fun handleEmptyList(state: Boolean) {
        setContentView(R.layout.empty_layout_for_no_item)
        setToolbar(state) // 툴바 설정
        findViewById<TextView>(R.id.alert_text).text = if (state) "팔로우가 없습니다." else "팔로워가 없습니다."
    }

    /**
     * 팔로우 상태에 따른 툴바 설정
     * @param state(Boolean): 팔로우 리스트(true)인지 팔로워 리스트(false)인지 구분
     * @author Seunggun Sin
     * @since 2022-07-28
     */
    private fun setToolbar(state: Boolean) {
        val toolbar = findViewById<Toolbar>(R.id.following_tool_bar)
        toolbar.title = if (state) "팔로잉 페이지" else "팔로워 페이지" // 툴바 타이틀 설정
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로 가기버튼 설정
    }

    /**
     * 가져온 리스트를 RecyclerView 의 어댑터에 적용하기
     * @param recyclerView(RecyclerView): 화면에 보여줄 recyclerView
     * @param list(List<FollowData>): 응답으로 받아온 팔로우(워) 목록 리스트
     * @author Seunggun Sin
     * @since 2022-07-28
     */
    private fun adaptList(recyclerView: RecyclerView, list: List<FollowData>) {
        list.forEachIndexed { index, element ->
            if (index == list.size - 1) { // 마지막 아이템인 경우에
                data.add(element)
                /*
                    특정 유저의 팔로우 or 팔로워 리스트에 내가 포함되어 있을 때 첫번째 인덱스로 이동
                 */
                for (followData in data) {
                    if (followData.userId == ResponseExistLogin.baseUserInfo?.userId) {
                        data.remove(followData) // 현재 데이터 제거
                        data.add(0, followData) // 첫번째 인덱스에 추가
                    }
                }
                val followAdapter = FollowAdapter(this, data)
                recyclerView.adapter = followAdapter // recyclerView 에 어댑터 적용
            } else {
                data.add(element) // 아이템 추가
            }
        }
    }

    /**
     * 가져온 리스트를 바탕으로 화면처리하기 (목록 화면, 빈 화면 처리)
     * @param size(Int): 리스트의 사이즈
     * @param state(Boolean): 팔로우 리스트(true)인지 팔로워 리스트(false)인지 구분
     * @param list(List<FollowData>): 응답으로 받아온 팔로우(워) 목록 리스트
     * @author Seunggun Sin
     * @since 2022-07-28
     */
    private fun showFollowList(size: Int, state: Boolean, list: List<FollowData>) {
        if (size == 0) { // 목록 없을 때
            handleEmptyList(state) // 빈 레이아웃 처리하기
        } else {
            setContentView(R.layout.activity_follow) // 목록 레이아웃 처리
            setToolbar(state) // 툴바 설정
            val recyclerView = findViewById<RecyclerView>(R.id.rcv_following)
            adaptList(recyclerView, list) // recyclerView 에 적용하기
        }
    }

    /**
     * 툴바 아이템 클릭 이벤트
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}