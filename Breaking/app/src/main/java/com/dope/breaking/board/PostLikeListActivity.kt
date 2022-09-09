package com.dope.breaking.board

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.R
import com.dope.breaking.adapter.FollowAdapter
import com.dope.breaking.databinding.ActivityFollowBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.FollowData
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.post.PostManager
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostLikeListActivity : AppCompatActivity() {
    private val TAG = "PostLikeActivity.kt"
    private var mbinding : ActivityFollowBinding? = null
    private val binding get() = mbinding!!
    private var likeList = mutableListOf<FollowData?>() // 좋아요 목록 저장하는 리스트
    private var isLoading = false // 로딩 중 판단
    private var isObtainedAll = false // 더 이상 얻을 리스트 있는지 판단
    private var postId = -1 // 좋아요 목록을 가져올 게시물 id
    private lateinit var adapterLikeList: FollowAdapter // 좋아요 리스트 어댑터 (팔로잉 어댑터 재사용)
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(this).getAccessTokenFromLocal()

        postId = intent.getIntExtra("postId",-1) // postId 가져오기

        progressBar = findViewById(R.id.progressbar_loading)
        recyclerView = findViewById(R.id.rcv_following)
        shimmerFrameLayout = findViewById(R.id.sfl_follow_list_skeleton)

        adapterLikeList = FollowAdapter(this, likeList, false, -1) // 초기 어댑터 지정

        /*
           최초 좋아요 리스트 요청
         */
        processGetPostLikeList(
            token,
            postId.toLong(),
            0, {
                showSkeletonView() // 스켈레톤 UI 시작
                isLoading = true
            },{
                adapterLikeList.addItems(it)

                if (likeList.size == 0) { // 목록 없을 때
                    handleEmptyList() // 빈 레이아웃 처리하기
                } else {
                    setToolbar() // 툴바 설정
                    extractMyselfItem() // 본인이 있다면 최상단으로 올리기
                }
                recyclerView.adapter = adapterLikeList // 리사이클러 뷰에 어댑터 설정

                /*
                  스크롤 이벤트 지정
                */
                recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)

                        val lastIndex =
                            (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                        // 가져온 아이템 사이즈가 가져와야하는 사이즈보다 작은 경우 새로운 요청을 못하게 막기
                        if (recyclerView.adapter!!.itemCount < ValueUtil.FOLLOW_SIZE) {
                            return
                        }

                        // 실제 데이터 리스트의 마지막 인덱스와 스크롤 이벤트에 의한 인덱스 값이 같으면서
                        // 스크롤이 드래깅 중이면서
                        // 피드 요청이 더 가능하면서
                        // 로딩 중이 아니라면
                        if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainedAll && !isLoading) {
                            processGetPostLikeList(
                                token,
                                postId.toLong(),
                                likeList[lastIndex]!!.cursorId, { // 마지막 인덱스
                                    adapterLikeList.addItem(null) // 로딩 아이템 추가
                                    isLoading = true // 로딩 상태 on
                                },
                                { it2 ->
                                    if (it2.size < ValueUtil.FOLLOW_SIZE) { // 정량으로 가져오는 개수보다 적다면
                                        adapterLikeList.removeLast() // 먼저 로딩 아이템 제거
                                        if (it2.isNotEmpty()) { // 리스트가 비어있지 않다면
                                            adapterLikeList.addItems(it2) // 받아온 리스트 추가
                                        }
                                        isObtainedAll = true // 더 이상 받아올 피드가 없다는 상태로 전환
                                    } else { // 리스트가 있다면
                                        adapterLikeList.removeLast() // 먼저 로딩 아이템 제거
                                        adapterLikeList.addItems(it2) // 받아온 리스트 추가
                                    }

                                    extractMyselfItem() // 새로 업데이트할 때마다 본인 아이템 있는 경우 최상단으로 옮기기
                                    isLoading = false // 로딩 상태 off
                                },
                                {
                                    DialogUtil().SingleDialog(
                                        this@PostLikeListActivity,
                                        "요청에 문제가 발생하였습니다.",
                                        "확인"
                                    ).show()
                                })
                        }
                    }
                })
                dismissSkeletonView() // 스켈레톤 UI 종료
                isLoading = false // 로딩 종료
            },{
                DialogUtil().SingleDialog(
                    this@PostLikeListActivity,
                    "요청에 문제가 발생하였습니다.",
                    "확인"
                ).show()
            })
    }

    /**
     * @description - 게시물 좋아요 리스트 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @param - postId(Long) : 좋아요 리스트를 요청할 게시물 id
     * @param - lastUserId(Int) : 가장 최근에 요청한 유저 id
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-06
     */
    private fun processGetPostLikeList(
        token: String,
        postId : Long,
        lastUserId : Int,
        init: () -> Unit,
        last: (List<FollowData>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ){
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기화 함수 호출
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startGetPostLikeList(
                    token,
                    postId,
                    lastUserId,
                    ValueUtil.LIKE_SIZE,
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
                Log.d(TAG, "좋아요 리스트 요청 결과 : $response")
            }catch (e: ResponseErrorException){
                error(e)
            }
        }
    }

    /**
     * 현재 리스트에서 본인이 포함되어있다면 맨 상단으로 이동시키는 함수
     * @author Seunggun
     * @since 2022-08-18
     */
    private fun extractMyselfItem() {
        var i = 0
        for (likeData in adapterLikeList.data) {
            if (likeData!!.userId == ResponseExistLogin.baseUserInfo?.userId && i != 0) {
                adapterLikeList.removeItem(likeData) // 현재 데이터 제거
                adapterLikeList.addItemIndex(0, likeData) // 첫번째 인덱스에 추가
                break
            }
            i++
        }
    }

    /**
     * 목록이 비어있을 때에 대한 화면 처리
     * @author Seunggun Sin | Tae hyun Park
     * @since 2022-07-28 | 2022-09-07
     */
    private fun handleEmptyList() {
        setContentView(R.layout.empty_layout_for_no_item)
        setToolbar() // 툴바 설정
        findViewById<TextView>(R.id.alert_text).text = "좋아요 목록이 없습니다."
    }

    /**
     * 좋아요 페이지 툴바 설정
     * @author Seunggun Sin | Tae hyun Park
     * @since 2022-07-28 | 2022-09-07
     */
    private fun setToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.following_tool_bar)
        toolbar.title = "좋아요 목록" // 툴바 타이틀 설정
        // 뒤로가기 버튼 설정
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_black_24)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * 스켈레톤 UI를 보여주는 함수 with shimmer effect
     * @author Seunggun Sin
     * @since 2022-08-25
     */
    private fun showSkeletonView() {
        recyclerView.visibility = View.GONE
        shimmerFrameLayout.visibility = View.VISIBLE
        shimmerFrameLayout.startShimmer()
    }

    /**
     * 스켈레톤 UI를 종료하는 함수
     * @author Seunggun Sin
     * @since 2022-08-25
     */
    private fun dismissSkeletonView() {
        shimmerFrameLayout.stopShimmer()
        shimmerFrameLayout.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
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