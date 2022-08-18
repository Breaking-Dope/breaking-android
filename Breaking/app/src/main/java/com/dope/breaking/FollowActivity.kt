package com.dope.breaking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
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
    private val followList = mutableListOf<FollowData?>() // 팔로우 목록 저장하는 리스트
    private var userId: Long = 0
    private val follow = Follow() // 팔로우 관련 기능 객체 생성
    private var isLoading = false // 로딩 중 판단
    private var isObtainedAll = false // 더 이상 얻을 리스트 있는지 판단
    private lateinit var followAdapter: FollowAdapter // 팔로우 리스트 어댑터
    private val cursorList = mutableListOf<Int>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
            ※ 팔로우, 팔로워 구분
            - 팔로우(팔로잉) 페이지 = true
            - 팔로워 페이지 = false
         */
        val state = intent.getBooleanExtra("state", true)
        userId = intent.getLongExtra("userId", 0) // 리스트를 보여주고자 하는 대상의 고유 id
        val requestToken =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(this).getAccessTokenFromLocal()

        setContentView(R.layout.activity_follow) // 목록 레이아웃 처리

        val progress = findViewById<ProgressBar>(R.id.progressbar_loading)
        val recyclerView = findViewById<RecyclerView>(R.id.rcv_following)

        followAdapter = FollowAdapter(this, followList, state, userId) // 초기 어댑터 지정

        /*
            최초 팔로우 리스트 요청
         */
        processGetFollowList(state, 0, requestToken, {
            // 초기 로딩 처리
            progress.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }, { it ->
            followAdapter.addItems(it) // 리스트에 추가하기
            followList.forEach {
                cursorList.add(it!!.cursorId)
            }

            if (followList.size == 0) { // 목록 없을 때
                handleEmptyList(state) // 빈 레이아웃 처리하기
            } else {
                setToolbar(state) // 툴바 설정
                extractMyselfItem() // 본인이 있다면 최상단으로 올리기
            }
            recyclerView.adapter = followAdapter // 리사이클러 뷰에 어댑터 설정

            // 로딩 처리 종료
            progress.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            /*
                스크롤 이벤트 지정
             */
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    val lastIndex =
                        (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                    if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainedAll && !isLoading) {
                        processGetFollowList(state,
                            followList[lastIndex]!!.cursorId, // 마지막 인덱스
                            requestToken,
                            {
                                followAdapter.addItem(null) // 로딩 아이템 추가
                                isLoading = true // 로딩 상태 on
                            },
                            { it2 ->
                                if (it2.size < ValueUtil.FOLLOW_SIZE) { // 리스트가 비어있다면
                                    followAdapter.removeLast() // 먼저 로딩 아이템 제거
                                    if (it2.isNotEmpty()) {
                                        followAdapter.addItems(it2) // 받아온 리스트 추가
                                    }
                                    isObtainedAll = true // 더 이상 받아올 피드가 없다는 상태로 전환
                                } else { // 리스트가 있다면
                                    followAdapter.removeLast() // 먼저 로딩 아이템 제거
                                    followAdapter.addItems(it2) // 받아온 리스트 추가
                                }

                                extractMyselfItem() // 새로 업데이트할 때마다 본인 아이템 있는 경우 최상단으로 옮기기
                                isLoading = false // 로딩 상태 off
                            },
                            {
                                DialogUtil().SingleDialog(
                                    this@FollowActivity,
                                    "요청에 문제가 발생하였습니다.",
                                    "확인"
                                ).show()
                            })
                    }
                }
            })
        }, {
            DialogUtil().SingleDialog(
                this@FollowActivity,
                "요청에 문제가 발생하였습니다.",
                "확인"
            ).show()
        })
    }

    /**
     * 현재 리스트에서 본인이 포함되어있다면 맨 상단으로 이동시키는 함수
     * @author Seunggun
     * @since 2022-08-18
     */
    private fun extractMyselfItem() {
        var i = 0
        for (followData in followAdapter.data) {
            if (followData!!.userId == ResponseExistLogin.baseUserInfo?.userId) {
                followAdapter.removeItem(followData) // 현재 데이터 제거
                followAdapter.addItemIndex(0, followData) // 첫번째 인덱스에 추가
                break
            }
            i++
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
        // 뒤로가기 버튼 설정
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_black_24)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * 팔로우(워) 리스트를 얻는 요청 프로세스 함수
     * @param state(Boolean): 팔로우인지, 팔로워 리스트인지 판단
     * @param cursorId(Int): 마지막으로 요청한 리스트의 마지막 인덱스
     * @param token(String): Jwt 엑세스 토큰
     * @param init(() -> Unit): 요청전 초기 실행 함수
     * @param last((List<FollowData>) -> Unit): 요청 후 실행할 함수
     * @param error((ResponseErrorException) -> Unit): 에러 발생 시 실행 함수
     * @author Seunggun Sin
     * @since 2022-08-18
     */
    private fun processGetFollowList(
        state: Boolean,
        cursorId: Int,
        token: String,
        init: () -> Unit,
        last: (List<FollowData>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기 함수 실행
            try {
                val responseList = if (state)
                    follow.startGetFollowingList(cursorId, token, userId)
                else
                    follow.startGetFollowerList(cursorId, token, userId)

                last(responseList) // 요청 후 함수 호출
            } catch (e: ResponseErrorException) {
                error(e) // 에러 함수 호출
            }
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