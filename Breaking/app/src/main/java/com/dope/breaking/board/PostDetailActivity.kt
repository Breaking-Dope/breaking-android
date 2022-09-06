package com.dope.breaking.board

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.dope.breaking.*
import com.dope.breaking.databinding.ActivityPostDetailBinding
import com.dope.breaking.databinding.CustomPostDetailContentPopupBinding
import com.dope.breaking.databinding.ItemSliderBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseComment
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.ResponsePostDetail
import com.dope.breaking.post.PostManager
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.Utils
import com.dope.breaking.util.ValueUtil
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.collections.ArrayList

class PostDetailActivity : AppCompatActivity() {
    private val TAG = "PostDetailActivity.kt"
    private var mbinding : ActivityPostDetailBinding? = null
    private val binding get() = mbinding!!
    private lateinit var editPostActivityResult: ActivityResultLauncher<Intent> // 게시글 수정 후 그 후 처리를 위한 activityResult
    private lateinit var adapterViewpager: ImageSliderAdapter // 뷰 페이저 어댑터
    private lateinit var adapterComment: PostCommentAdapter // 댓글 리스트 어댑터
    private lateinit var progressDialog : DialogUtil.ProgressDialog // 요청 로딩 다이얼로그
    private var commentList = mutableListOf<ResponseComment?>() // 댓글 리스트
    private var flagLike = false // 좋아요가 안 눌렸으면 false, 눌렸으면 true (임시)
    private var flagComment = false // 댓글 창을 안 눌렀으면 false, 눌렀으면 true (임시)
    private var isObtainedAll = false // 모든 댓글 리스트를 받았는지 판단(더 이상 요청할 것이 없는)
    private var isLoading = false  // 로딩 중 판단
    private var requestCommentId = -1 // 대댓글 요청 시에 보낼 댓글 id (기본값은 임의로 -1로 설정)
    private var getPostId = -1 // 게시물 id

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 댓글 요청 에러 시 띄워줄 다이얼로그 정의
        val requestErrorDialog =
            DialogUtil().SingleDialog(applicationContext, "댓글을 가져오는데 문제가 발생하였습니다.", "확인")

        // 요청 Jwt 토큰 가져오기
        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(applicationContext).getAccessTokenFromLocal()

        getPostId = intent.getIntExtra("postId",-1)
        Log.d(TAG,"받아온 postId 값 : $getPostId")

        mbinding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingPostToolBar()  // 툴 바 설정
        allowScrollEditText() // 스크롤 바 중첩 문제 해결

        // 게시글 상세 조회 요청
        processPostDetail(
            token,
            getPostId.toLong(), {
                showSkeletonView() // 스켈레톤 UI 시작
            },{
                dismissSkeletonView() // 스켈레톤 UI 종료
                settingPostDetailView(it) // 받아온 it을 바탕으로 view에 뿌려주기
            }
        )

        binding.viewPager.offscreenPageLimit = 1 // 한 페이지에 한 이미지만 보여주도록 설정
        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){ // viewPager2 페이지 변화 감지
            @Override
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
            }
        })

        // 좋아요 클릭 시 토글
        binding.ibPostLike.setOnClickListener {
            if (!flagLike) {
                binding.ibPostLike.backgroundTintList = ColorStateList.valueOf(Color.BLUE)
                flagLike = true
            } else {
                binding.ibPostLike.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
                flagLike = false
            }
        }

        // 댓글 클릭 시 토글
        binding.ibPostComment.setOnClickListener {
            if (!flagComment) {
                binding.ibPostComment.backgroundTintList = ColorStateList.valueOf(Color.BLUE)
                flagComment = true
            } else {
                binding.ibPostComment.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
                flagComment = false
            }
        }

        // 댓글 등록 버튼 클릭 시
        binding.tvAddComment.setOnClickListener {
            if (binding.etPostWrite.text.isNotEmpty()){
                // commentId가 -1인지 아닌지 체크하여, -1이 아니면 대댓글 등록 요청
                if(requestCommentId != -1){ // -1이 아니라면 대댓글 요청
                    processWriteNestedComment(
                        token,
                        requestCommentId.toLong(),
                        binding.etPostWrite.text.toString(),
                        Utils.getArrayHashTagWithOutSpace(binding.etPostWrite.text.toString()), // 해시태그 (없으면 빈 리스트로 전달해주는 상황)
                        {},{
                            if (it){
                                Toast.makeText(applicationContext, "답글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                                commentList.clear() // 리스트 비우기
                                binding.etPostWrite.setText("") // 적었던 내용은 지우기
                                requestCommentId = -1 // 값 다시 초기화

                                // 게시글 상세 조회 갱신
                                processPostDetail(
                                    token,
                                    getPostId.toLong(), {
                                    },{
                                        settingPostDetailView(it) // 받아온 it을 바탕으로 view에 뿌려주기
                                    }
                                )
                                // 대댓글 등록 후 댓글 리스트 재갱신
                                processGetCommentListModule(token, getPostId.toLong(), requestErrorDialog)
                            }
                        }
                    )
                }else{ // -1이면 일반 댓글 요청
                    processWriteComment(
                        token,
                        getPostId.toLong(),
                        binding.etPostWrite.text.toString(),
                        Utils.getArrayHashTagWithOutSpace(binding.etPostWrite.text.toString()), // 해시태그 (없으면 빈 리스트로 전달해주는 상황)
                        {},{
                            if (it){ // 댓글 등록이 성공적으로 이루어졌다면
                                binding.etPostWrite.setText("") // 적었던 내용은 지우기
                                commentList.clear() // 리스트 비우기
                                Toast.makeText(applicationContext, "댓글이 작성되었습니다.", Toast.LENGTH_SHORT).show()

                                // 게시글 상세 조회 갱신
                                processPostDetail(
                                    token,
                                    getPostId.toLong(), {
                                    },{
                                        settingPostDetailView(it) // 받아온 it을 바탕으로 view에 뿌려주기
                                    }
                                )
                                // 댓글 등록 후 댓글 리스트 재갱신
                                processGetCommentListModule(token, getPostId.toLong(), requestErrorDialog)
                            }
                        }
                    )
                }
            }else{
                Toast.makeText(applicationContext, "댓글을 입력해주세요!", Toast.LENGTH_SHORT).show()
            }
        }

        /*
        최초로 댓글 리스트 가져오는 요청
         */
        processGetCommentListModule(token, getPostId.toLong(), requestErrorDialog)

        /* 댓글 RecyclerView 스크롤 이벤트 리스너 정의 */
        binding.rvCommentList.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // 스크롤하면서 리스트의 가장 마지막 위치에 도달했을 때, 그 인덱스 값 가져오기
                val lastIndex =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                // 가져온 아이템 사이즈가 가져와야하는 사이즈보다 작은 경우 새로운 요청을 못하게 막기
                if (recyclerView.adapter!!.itemCount < ValueUtil.COMMENT_SIZE) {
                    return
                }

                // 실제 데이터 리스트의 마지막 인덱스와 스크롤 이벤트에 의한 인덱스 값이 같으면서
                // 스크롤이 드래깅 중이면서
                // 댓글 리스트 요청이 더 가능하면서
                // 로딩 중이 아니라면
                if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainedAll && !isLoading) {
                    processGetCommentList(token, getPostId.toLong(), commentList[lastIndex]!!.commentId, {
                        adapterComment.addItem(null) // 로딩 창 아이템 추가
                        isLoading= true // 로딩 시작 상태로 전환
                    },{
                        if(it.size < ValueUtil.COMMENT_SIZE){ // 정량으로 가져오는 개수보다 적다면
                            adapterComment.removeLast() // 로딩 아이템 제거
                            if (it.isNotEmpty()) // 리스트가 있다면
                                adapterComment.addItems(it)
                            isObtainedAll = true // 더 이상 받아올 댓글이 없는 상태
                        }else{ // 더 요청할 수 있고 받아온 리스트가 있다면
                            adapterComment.removeLast() // 로딩 아이템 제거
                            adapterComment.addItems(it) // 받아온 리스트 추가
                        }
                        isLoading = false // 로딩 종료 상태로 전환
                    },{
                        // BSE451 에러의 경우 더 이상의 받아올 댓글 리스트가 없는 경우 발생
                        if (it.message!!.contains("BSE451")) {
                            isObtainedAll = true
                            adapterComment.removeLast()
                        }
                    })
                }
            }
        })

        // 게시글 수정 완료 후 다시 상세 페이지로 다시 돌아오면
        editPostActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    var resultPostId = it.data?.getIntExtra("postId", -1) // 수정이 정상적으로 완료되었다면 기존 게시물과 동일한 id 받아옴
                    if(resultPostId != -1){ // 수정이 완료되었다면 상세 뷰, 댓글 리스트 재갱신
                        // 게시글 상세 조회
                        processPostDetail(
                            token,
                            resultPostId!!.toLong(), {
                                showSkeletonView() // 스켈레톤 UI 시작
                            },{
                                dismissSkeletonView() // 스켈레톤 UI 종료
                                settingPostDetailView(it) // 받아온 it을 바탕으로 view에 뿌려주기
                            }
                        )
                        // 댓글 리스트 갱신
                        commentList.clear()
                        processGetCommentListModule(token, getPostId.toLong(), requestErrorDialog)
                    }
                }
            }
    }

    override fun onStop() {
        super.onStop()
        // 소리 줄이기 (영상 미디어, 임시 방편)
        /*
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0,
            AudioManager.FLAG_PLAY_SOUND
        )*/
        adapterViewpager.onDetach(ItemSliderBinding.inflate(layoutInflater)) // 자원 해제
    }

    override fun onDestroy() {
        super.onDestroy()
        adapterViewpager.onDetach(ItemSliderBinding.inflate(layoutInflater)) // 자원 해제
    }

    override fun onBackPressed() {
        intent.putExtra("isRefreshFeed",true) // 세부 조회 페이지로 이동, true 는 메인 피드를 재갱신하라는 의미
        setResult(RESULT_OK, intent)
        finish() // 다시 세부 조회 페이지로 이동
    }

    /**
     * @description - 최초 댓글 리스트 불러오기, 댓글/대댓글 작성 후에 리스트를 재갱신하기 위해 재사용되는 메소드
     * @param - token(String) : 토큰 정보
     * @param - postId(Long) : 현재 게시물 id
     * @param - requestErrorDialog(DialogUtil.SingleDialog) : 요청 에러 다이얼로그
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-03
     */
    private fun processGetCommentListModule(token: String, postId: Long, requestErrorDialog: DialogUtil.SingleDialog){
        processGetCommentList(token, postId,0, {
            isObtainedAll = false
        }, { it ->
            commentList.addAll(it) // 어댑터에 넣어줄 댓글 리스트 데이터
            adapterComment =
                PostCommentAdapter(applicationContext, commentList, token, binding) // 어댑터 정의
            adapterComment.setItemReplyClickListener(object : PostCommentAdapter.OnItemClickListener{ // 답글 달기 클릭 리스너 등록
                override fun onClick(v: View, position: Int) {
                    Log.d(TAG,"누른 댓글 id 테스트 : ${commentList[position]?.commentId}")
                    requestCommentId = if(v.findViewById<TextView>(R.id.tv_post_reply).text.equals("답글")) // 댓글 작성 중이 아니면
                        -1 // 다시 초기화
                    else
                        commentList[position]?.commentId!!
                    Log.d(TAG, "requestCommentId : $requestCommentId")
                }
            })
            if (it.isEmpty()) { // 리스트가 비어있다면
                binding.tvCommentNone.visibility = View.VISIBLE
                binding.rvCommentList.visibility = View.GONE
            } else { // 있다면
                binding.tvCommentNone.visibility = View.GONE
                binding.rvCommentList.visibility = View.VISIBLE
            }
            // 리스트의 divider 선 추가
            binding.rvCommentList.addItemDecoration(
                DividerItemDecoration(
                    applicationContext,
                    LinearLayout.VERTICAL
                )
            )
            binding.rvCommentList.adapter = adapterComment // 어댑터 지정
        },{
            it.printStackTrace()
            requestErrorDialog.show()
        })
    }

    /**
     * @description - 게시글 내용의 더보기 메뉴 클릭 시 커스텀 팝업 메뉴를 보여주는 함수
     * @param - responsePostDetail(ResponsePostDetail) : 게시글 세부 조회 DTO
     * @return - None
     * @author - Seung gun Sin | Tae hyun Park
     * @since - 2022-08-24
     */
    private fun setMoreMenuContent(responsePostDetail: ResponsePostDetail){
        // 게시물 삭제 요청 에러 시 띄워줄 다이얼로그 정의
        val requestErrorDialog =
            DialogUtil().SingleDialog(applicationContext, "게시글을 삭제에 문제가 발생하였습니다.", "확인")

        val popupInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBind =
            CustomPostDetailContentPopupBinding.inflate(popupInflater) // 커스텀 팝업 레이아웃 binding inflate
        val popupWindow = PopupWindow(
            popupBind.root,
            ViewGroup.LayoutParams.WRAP_CONTENT, // 가로 길이
            ViewGroup.LayoutParams.WRAP_CONTENT, // 세로 길이
            true
        ) // 팝업 윈도우 화면 설정

        binding.ibPostMore.setOnClickListener(popupWindow::showAsDropDown) // 더보기 메뉴 클릭 시, 메뉴 view 중심으로 팝업 메뉴 호출
        if(responsePostDetail.user?.userId == ResponseExistLogin.baseUserInfo?.userId || responsePostDetail.isMyPost){ // 해당 게시글 작성자가 본인이면
            binding.btnPurchase.text = "구매자 목록"
            if(responsePostDetail.soldCount > 0){ // 게시물이 판매되었다면 수정/삭제가 불가함.
                // 채팅 메뉴 비활성화
                popupBind.layoutHorizChat.visibility = View.GONE
                // 차단 메뉴 비활성화
                popupBind.layoutHorizBan.visibility = View.GONE
                // 수정 메뉴 비활성화
                popupBind.layoutHorizEdit.visibility = View.GONE
                // 삭제 메뉴 비활성화
                popupBind.layoutHorizDelete.visibility = View.GONE
                // 비활성화 메뉴 보이게
                popupBind.layoutHorizDeactivation.visibility = View.VISIBLE
            }else{
                // 채팅 메뉴 비활성화
                popupBind.layoutHorizChat.visibility = View.GONE
                // 차단 메뉴 비활성화
                popupBind.layoutHorizBan.visibility = View.GONE
                // 수정 메뉴 활성화
                popupBind.layoutHorizEdit.visibility = View.VISIBLE
                // 삭제 메뉴 활성화
                popupBind.layoutHorizDelete.visibility = View.VISIBLE
                // 비활성화 메뉴 보이게
                popupBind.layoutHorizDeactivation.visibility = View.VISIBLE
            }
        }else{ // 다른 사람의 게시물이라면
            binding.btnPurchase.text = "구매하기"
            // 수정 메뉴 비활성화
            popupBind.layoutHorizEdit.visibility = View.GONE
            // 삭제 메뉴 비활성화
            popupBind.layoutHorizDelete.visibility = View.GONE
            // 채팅 메뉴 활성화
            popupBind.layoutHorizChat.visibility = View.VISIBLE
            // 차단 메뉴 활성화
            popupBind.layoutHorizBan.visibility = View.VISIBLE
            // 비활성화 메뉴 안보이게
            popupBind.layoutHorizDeactivation.visibility = View.GONE
        }

        // 채팅 메뉴 클릭 시
        popupBind.layoutHorizChat.setOnClickListener {
            popupWindow.dismiss()
        }

        // 차단 메뉴 클릭 시
        popupBind.layoutHorizBan.setOnClickListener {
            popupWindow.dismiss()
        }

        // 북마크 메뉴 클릭 시
        popupBind.layoutHorizBookmark.setOnClickListener {
            popupWindow.dismiss()
        }

        // 공유 메뉴 클릭 시
        popupBind.layoutHorizShare.setOnClickListener {
            popupWindow.dismiss()
        }

        // 수정 메뉴 누르면 수정 페이지로 이동
        popupBind.layoutHorizEdit.setOnClickListener {
            popupWindow.dismiss()
            var intent = Intent(applicationContext, EditPostActivity::class.java)
            intent.putExtra("postInfo", responsePostDetail) // 수정 전 게시글 정보 전달
            intent.putExtra("postId",getPostId) // 수정할 게시글 id
            editPostActivityResult.launch(intent)
        }

        // 삭제 메뉴 클릭 시 게시글 삭제
        popupBind.layoutHorizDelete.setOnClickListener {
            // 사용자에게 현재 위치로 선택할 것인지 물어보기
            DialogUtil().MultipleDialog(
                this,
                "게시물을 삭제하시겠습니까?",
                "예",
                "아니오",
                {
                    // 게시글 삭제 요청 함수 시작
                    processDeletePost(
                        ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(applicationContext).getAccessTokenFromLocal(),
                        getPostId.toLong(),{
                            // 게시글 삭제 요청 다이얼 로그 시작
                            progressDialog = DialogUtil().ProgressDialog(this)
                            progressDialog.showDialog()
                        },{
                            if (progressDialog.isShowing()) // 로딩 다이얼로그 종료
                                progressDialog.dismissDialog()
                            if (it){
                                intent.putExtra("isDeletePost",true)
                                setResult(RESULT_OK, intent)
                                finish() // 게시글 세부 조회 페이지 종료, 메인 피드로 화면 돌아감.
                            }
                        },{
                            it.printStackTrace()
                            requestErrorDialog.show()
                        }
                    )
                },
                { popupWindow.dismiss() }).show()
        }

        // 비활성화 메뉴 클릭 시
        popupBind.layoutHorizDeactivation.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    /**
     * @description - 미디어 리스트의 개수만큼 Indicator 를 만들어서 초기화시키는 함수
     * @param - count(Int) : 상세 조회로 받아온 미디어 리스트의 개수
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-24
     */
    private fun setupIndicators(count: Int){
        binding.layoutIndicators.removeAllViews() // 중복 방지를 위해 뷰 비우기
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(16,8,16,8)
        for(i in 0 until count){
            var indicators = ImageView(this)
            indicators.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.bg_indicator_inactive))
            indicators.layoutParams = params
            binding.layoutIndicators.addView(indicators)
        }
        setCurrentIndicator(0)
    }

    /**
     * @description - 뷰 페이저를 통해 넘긴 사진 페이지 값을 받아와 Indicator 에 변화를 주는 메소드
     * @param - position(Int) : 넘긴 현재 페이지의 위치 값
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-24
     */
    private fun setCurrentIndicator(position: Int){
        val childCount = binding.layoutIndicators.childCount
        for(i in 0 until childCount){
            val imageView = binding.layoutIndicators.getChildAt(i) as ImageView
            if(i == position){
                imageView.setImageDrawable(ContextCompat.getDrawable(
                    this,
                    R.drawable.bg_indicator_active
                ))
            }else{
                imageView.setImageDrawable(ContextCompat.getDrawable(
                    this,
                    R.drawable.bg_indicator_inactive
                ))
            }
        }
    }

    /**
     * @description - JWT 토큰과 게시글 ID를 통해 게시글 세부 조회 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-15
     */
    private fun processPostDetail(
        token: String,
        postId: Long,
        init: () -> Unit,
        last: (ResponsePostDetail) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기화 함수 호출
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val responsePostDetail = postManager.startGetPostDetail(
                    token,
                    postId
                )
                Log.d(TAG, "요청 성공 시 받아온 게시물 제목 : ${responsePostDetail.title}\n" +
                        "isMyPost값 : ${responsePostDetail.isMyPost}")
                last(responsePostDetail) // 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                e.printStackTrace()
                DialogUtil().SingleDialog(
                    applicationContext,
                    "게시글 상세 조회 요청에 문제가 발생하였습니다.",
                    "확인"
                )
            }
        }
    }

    /**
     * @description - 게시글의 댓글 등록 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @param - postId(Long) : 댓글을 작성할 게시물 id
     * @param - content(String) : 댓글 내용
     * @param - hashTagList(ArrayList<String>) : 댓글의 해시태그 리스트
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-01
     */
    private fun processWriteComment(
        token: String,
        postId: Long,
        content: String,
        hashTagList: ArrayList<String>?,
        init: () -> Unit,
        last: (Boolean) -> Unit
    ){
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기화 함수 호출
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startRegisterComment(
                    token,
                    postId,
                    content,
                    hashTagList
                )
                if (response) last(response)
                Log.d(TAG, "댓글 등록 요청 결과 : $response")
            }catch (e: ResponseErrorException){
                e.printStackTrace()
                DialogUtil().SingleDialog(
                    applicationContext,
                    "댓글 작성 요청에 문제가 발생하였습니다.",
                    "확인"
                )
            }
        }
    }

    /**
     * @description - 게시글의 대댓글 등록 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @param - commentId(Long) : 대댓글을 작성할 댓글의 id
     * @param - content(String) : 대댓글 내용
     * @param - hashTagList(ArrayList<String>) : 대댓글의 해시태그 리스트
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-02
     */
    private fun processWriteNestedComment(
        token: String,
        commentId: Long,
        content: String,
        hashTagList: ArrayList<String>?,
        init: () -> Unit,
        last: (Boolean) -> Unit
    ){
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기화 함수 호출
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startRegisterNestedComment(
                    token,
                    commentId,
                    content,
                    hashTagList
                )
                if (response) last(response)
                Log.d(TAG, "대댓글 등록 요청 결과 : $response")
            }catch (e: ResponseErrorException){
                e.printStackTrace()
                DialogUtil().SingleDialog(
                    applicationContext,
                    "대댓글 작성 요청에 문제가 발생하였습니다.",
                    "확인"
                )
            }
        }
    }

    /**
     * @description - 게시물의 댓글 리스트 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @param - postId(Long) : 댓글 리스트를 요청할 게시물 id
     * @param - lastCommentId(Int) : 가장 최근에 요청한 댓글 id
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-01
     */
    private fun processGetCommentList(
        token: String,
        postId : Long,
        lastCommentId : Int,
        init: () -> Unit,
        last: (List<ResponseComment>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ){
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기화 함수 호출
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startGetCommentList(
                    token,
                    postId,
                    lastCommentId,
                    ValueUtil.COMMENT_SIZE,
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
                Log.d(TAG, "댓글 리스트 요청 결과 : $response")
            }catch (e: ResponseErrorException){
                error(e)
            }
        }
    }

    /**
     * @description - 게시물 삭제 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @param - postId(Long) : 삭제를 요청할 게시물 id
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-05
     */
    private fun processDeletePost(
        token: String,
        postId : Long,
        init: () -> Unit,
        last: (Boolean) -> Unit,
        error: (ResponseErrorException) -> Unit
    ){
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기화 함수 호출
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startDeletePost(
                    token,
                    postId,
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            }catch (e: ResponseErrorException){
                error(e)
            }
        }
    }


    /**
     * @description - 받아온 상세 조회 정보를 바탕으로 뷰에 보여주기
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-18 | 2022-08-25
     */
    @SuppressLint("ResourceAsColor")
    private fun settingPostDetailView(responsePostDetail: ResponsePostDetail){
        // 툴 바 수정 버튼 뷰 처리
        setViewNickNameProfile(responsePostDetail) // 작성자의 닉네임과 프로필 이미지, 댓글 프로필 이미지
        setViewPostTypeSold(responsePostDetail) // 게시글 타입 (단독, 판매 중, 판매완료)
        setViewPostContents(responsePostDetail) // 게시글의 주요 컨텐츠 (제목, 위치, 시간, 가격, 내용)
        setViewHashTag(responsePostDetail) // 게시글의 해시태그를 강조
        setViewMediaList(responsePostDetail) // 게시글 미디어 처리
        setMoreMenuContent(responsePostDetail) // 커스텀 팝업 다이얼로그 설정
    }

    /**
     * @description - 게시글 작성자의 닉네임, 작성자 프로필 이미지, 댓글 프로필 이미지를 보여주는 함수 (닉네임&프로필 담당 함수)
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-25
     */
    private fun setViewNickNameProfile(responsePostDetail: ResponsePostDetail){
        if (responsePostDetail.isAnonymous){ // 익명이면 기본 default 프로필 이미지와 닉네임을 익명으로 표시
            // 게시글 작성자의 프로필 이미지
            Glide.with(applicationContext)
                .load(R.drawable.ic_default_profile_image)
                .placeholder(R.drawable.ic_default_profile_image)
                .error(R.drawable.ic_default_profile_image)
                .circleCrop()
                .into(binding.ivProfileWriter)
            // 유저 닉네임
            binding.tvUserNickName.text = "익명"
        }else{ // 익명이 아니면 게시글 작성자의 프로필 이미지로 보여주기
            // 게시글 작성자의 프로필 이미지
            Glide.with(applicationContext)
                .load(ValueUtil.IMAGE_BASE_URL + responsePostDetail.user?.profileImgUrl)
                .placeholder(R.drawable.ic_default_profile_image)
                .error(R.drawable.ic_default_profile_image)
                .circleCrop()
                .into(binding.ivProfileWriter)
            // 유저 닉네임
            binding.tvUserNickName.text = responsePostDetail.user?.nickname
        }
        // 댓글 프로필은 현재 로그인한 내 프로필을 보여줌
        Glide.with(applicationContext)
            .load(ValueUtil.IMAGE_BASE_URL + ResponseExistLogin.baseUserInfo?.profileImgUrl)
            .circleCrop()
            .into(binding.ivCommentWriterProfile)
    }

    /**
     * @description - 해당 게시글이 판매 완료인지, 판매 중인지, 단독 제보인지 구분하고 보여주는 함수 (게시글 타입 담당 함수)
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-25
     */
    private fun setViewPostTypeSold(responsePostDetail: ResponsePostDetail){
        // 게시글 타입 (단독, 판매완료, 판매중)
        if(responsePostDetail.postType != "EXCLUSIVE") // 단독 제보가 아니라면
            binding.tvChipExclusive.visibility = View.GONE // 단독 제보 비활성화
        else
            binding.tvChipExclusive.visibility = View.VISIBLE // 다시 활성화

        if(responsePostDetail.isSold){ // 판매 완료라면
            binding.tvChipSold.visibility = View.VISIBLE // 판매 완료 다시 활성화
            binding.tvChipUnsold.visibility = View.GONE // 판매 중 비활성화
        }else{
            binding.tvChipUnsold.visibility = View.VISIBLE // 판매 중 다시 활성화
            binding.tvChipSold.visibility = View.GONE // 판매 완료 비활성화
        }
    }

    /**
     * @description - 해당 게시글의 상단 박스 영역의 제목, 위치, 시간, 가격, 누적 판매량 등 주요 내용을 보여주는 함수 (게시글 주요 컨텐츠 담당 함수)
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-25
     */
    private fun setViewPostContents(responsePostDetail: ResponsePostDetail){
        // 게시글 제보 위치
        binding.tvPostLocation.text = responsePostDetail.location.region_2depth_name
        // 게시글 제목
        binding.tvTitle.text = responsePostDetail.title
        // 게시글 작성 내용
        binding.tvPostContent.text = responsePostDetail.content
        // 게시글 가격
        if(responsePostDetail.price.toInt() == 0){ // 0원이면 무료라고 표시
            binding.tvPostPrice.text = "무료"
        }else{ // 무료가 아니면 포맷 맞춰서 표시
            val dec = DecimalFormat("#,###")
            binding.tvPostPrice.text = dec.format(responsePostDetail.price) + "원"
        }
        // 게시글 사건 발생 시간
        var postTimeList = responsePostDetail.eventDate.split("T").toTypedArray()
        var postFirstList = postTimeList[0].split("-").toTypedArray()
        var postSecondList = postTimeList[1].split(":").toTypedArray()
        binding.tvPostEventTime.text = "발생시간 ${postFirstList[0]}.${postFirstList[1]}.${postFirstList[2]}. ${postSecondList[0]}:${postSecondList[1]}"

        // 게시글 작성 시간
        postTimeList = responsePostDetail.createdDate.split("T").toTypedArray()
        postFirstList = postTimeList[0].split("-").toTypedArray()
        postSecondList = postTimeList[1].split(":").toTypedArray()
        binding.tvPostCreateTime.text = "작성시간 ${postFirstList[0]}.${postFirstList[1]}.${postFirstList[2]}. ${postSecondList[0]}:${postSecondList[1]}"

        // 누적 판매량
        binding.tvPostSoldCount.text = "누적 판매 ${responsePostDetail.soldCount}"
        // 좋아요 수
        binding.tvPostLikeCount.text = responsePostDetail.likeCount.toString()
        // 댓글 수
        binding.tvPostCommentCount.text = responsePostDetail.commentCount.toString()
        // 조회 수
        binding.tvPostViewCount.text = "· 조회수 ${responsePostDetail.viewCount}"
    }

    /**
     * @description - 해당 게시글의 해시태그 리스트를 받아와 본문에서 해시태그를 강조하여 보여주는 함수 (게시글 해시태그 담당 함수)
     * @param - None
     * @author - Tae hyun Park
     * @since - 2022-08-25
     * @return - None
     */
    private fun setViewHashTag(responsePostDetail: ResponsePostDetail){
        // 해시 태그 리스트 색상 표시
        Log.d(TAG, "해당 게시물의 해시 태그 리스트 : ${responsePostDetail.hashtagList}")
        Log.d(TAG, "해당 게시물의 전체 컨텐츠 : ${responsePostDetail.content}")
        var spannableString = SpannableString(responsePostDetail.content) // 텍스트 뷰의 특정 문자열 처리를 위한 spannableString 객체 생성
        var startList = ArrayList<Int>()
        for(hashString in responsePostDetail.hashtagList){
            var start = responsePostDetail.content.indexOf("#$hashString") // 전체 문자열에서 해당 해시태그 문자열과 일치하는 첫 인덱스를 찾아낸다
            for(listIndex in startList){
                if(start == listIndex)// 중복된 태그가 이미 있다면
                    start = responsePostDetail.content.indexOf("#$hashString",start+1) // 중복이므로 그 다음 인덱스부터 다시 찾는다
            }
            startList.add(start) // 인덱스들을 저장
            var end = start + hashString.length // 해당 해시태그 문자열의 끝 인덱스
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#014D91")),
                start,
                end + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // spannable 속성 지정
        }
        startList.clear()
        binding.tvPostContent.text = spannableString
    }

    /**
     * @description - 해당 게시글의 등록된 미디어 이미지들을 viewPager2를 이용하여 보여주는 함수 (게시글 미디어 담당 함수)
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-25
     */
    private fun setViewMediaList(responsePostDetail: ResponsePostDetail){
        Log.d(TAG,"미디어 리스트 URL : ${responsePostDetail.mediaList}")
        // 받아온 mediaList 처리
        if(responsePostDetail.mediaList.size == 0){ // 게시물의 이미지가 없다면
            binding.ivPostDetailDefault.visibility = View.VISIBLE // default 이미지 보여주기
        }else{ // 게시물의 이미지가 있다면
            binding.ivPostDetailDefault.visibility = View.GONE // default 이미지 없애기
            adapterViewpager = ImageSliderAdapter(
                this,
                responsePostDetail.mediaList) // viewPager2 어댑터 세팅
            binding.viewPager.adapter = adapterViewpager
            setupIndicators(responsePostDetail.mediaList.size) // linearLayout 에 indicators 초기화
        }
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
                intent.putExtra("isRefreshFeed",true) // 세부 조회 페이지로 이동, true 는 메인 피드를 재갱신하라는 의미
                setResult(RESULT_OK, intent)
                finish() // 다시 세부 조회 페이지로 이동
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * @description - activity_post_detail.xml에서 이미 최상위 NestedScrollView가 정의되어 있기 때문에 EditText에 스크롤 옵션을 주어도 이벤트가 막히는 현상이 발생한다.
    따라서 해당 메소드를 통해 EditText 가 터치되어있을 때 부모의 스크롤 권한을 가로채고, EditText가 아닌 바깥을 터치한다면 다시 부모 스크롤 뷰가 동작하도록
    하는 메소드.
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-24
     */
    private fun allowScrollEditText() {
        // EditText 스크롤 터지 이벤트 리스너
        binding.etPostWrite.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (v!!.id === R.id.et_post_write) { // 안쪽 EditText 를 클릭했다면
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
     * 스켈레톤 UI를 보여주는 함수 with shimmer effect
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    private fun showSkeletonView() {
        binding.rvCommentList.visibility = View.GONE // 댓글 리스트 invisible
        binding.viewWholeContentLayout.visibility = View.GONE // 전체 컨텐츠 invisible
        binding.sflPostDetailSkeleton.visibility = View.VISIBLE // 스켈레톤 visible
        binding.sflPostDetailSkeleton.startShimmer()
    }

    /**
     * 스켈레톤 UI를 종료하는 함수
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    private fun dismissSkeletonView() {
        binding.sflPostDetailSkeleton.stopShimmer()
        binding.sflPostDetailSkeleton.visibility = View.GONE // 스켈레톤 gone
        binding.rvCommentList.visibility = View.VISIBLE      // 댓글 리스트 visible
        binding.viewWholeContentLayout.visibility = View.VISIBLE // 전체 컨텐츠 visible
        binding.tvPostContent.visibility = View.VISIBLE
    }
}