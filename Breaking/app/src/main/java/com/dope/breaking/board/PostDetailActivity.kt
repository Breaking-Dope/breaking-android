package com.dope.breaking.board

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.dope.breaking.R
import com.dope.breaking.databinding.ActivityPostDetailBinding
import com.dope.breaking.databinding.CustomPostDetailContentPopupBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.ResponsePostDetail
import com.dope.breaking.post.PostManager
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.Utils
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.ArrayList

class PostDetailActivity : AppCompatActivity() {
    private val TAG = "PostDetailActivity.kt"
    private var mbinding : ActivityPostDetailBinding? = null
    private val binding get() = mbinding!!
    private lateinit var adapter: ImageSliderAdapter // 뷰 페이저 어댑터
    private var flagLike = false // 좋아요가 안 눌렸으면 false, 눌렸으면 true (임시)
    private var flagComment = false // 댓글 창을 안 눌렀으면 false, 눌렀으면 true (임시)

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingPostToolBar()  // 툴 바 설정
        allowScrollEditText() // 스크롤 바 중첩 문제 해결

        var getPostId = intent.getIntExtra("postId",-1)
        Log.d(TAG,"받아온 postId 값 : $getPostId")

        // 게시글 상세 조회 요청
        processPostDetail(
            JwtTokenUtil(applicationContext).getAccessTokenFromLocal(), // 로컬에서 토큰 가져오기
            getPostId.toLong(), {
                showSkeletonView() // 스켈레톤 UI 시작
            },{
                dismissSkeletonView() // 스켈레톤 UI 종료
                settingPostDetailView(it) // 받아온 it을 바탕으로 view에 뿌려주기
                Log.d(TAG,"visibility 테스트 : ${binding.tvPostContent.visibility}")
                Log.d(TAG,"값 테스트 : ${binding.tvPostContent.text}")
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
    }

    /**
     * @description - 게시글 내용의 더보기 메뉴 클릭 시 커스텀 팝업 메뉴를 보여주는 함수
     * @param - responsePostDetail(ResponsePostDetail) : 게시글 세부 조회 DTO
     * @return - None
     * @author - Seung gun Sin | Tae hyun Park
     * @since - 2022-08-24
     */
    private fun setMoreMenuContent(responsePostDetail: ResponsePostDetail){
        val popupInflater =
            applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBind =
            CustomPostDetailContentPopupBinding.inflate(popupInflater) // 커스텀 팝업 레이아웃 binding inflate

        val popupWindow = PopupWindow(
            popupBind.root,
            ViewGroup.LayoutParams.WRAP_CONTENT, // 가로 길이
            ViewGroup.LayoutParams.WRAP_CONTENT, // 세로 길이
            true
        ) // 팝업 윈도우 화면 설정

        binding.ibPostMore.setOnClickListener(popupWindow::showAsDropDown) // 더보기 메뉴 클릭 시, 메뉴 view 중심으로 팝업 메뉴 호출

        if(responsePostDetail.user?.userId == ResponseExistLogin.baseUserInfo?.userId){ // 해당 게시글 작성자가 본인이면
            // 수정 메뉴 활성화
            popupBind.layoutHorizEdit.visibility = View.VISIBLE
            // 삭제 메뉴 활성화
            popupBind.layoutHorizDelete.visibility = View.VISIBLE
            // 채팅 메뉴 비활성화
            popupBind.layoutHorizChat.visibility = View.GONE
            // 차단 메뉴 비활성화
            popupBind.layoutHorizBan.visibility = View.GONE
        }else{ // 다른 사람의 게시물이라면
            // 수정 메뉴 비활성화
            popupBind.layoutHorizEdit.visibility = View.GONE
            // 삭제 메뉴 비활성화
            popupBind.layoutHorizDelete.visibility = View.GONE
            // 채팅 메뉴 활성화
            popupBind.layoutHorizChat.visibility = View.VISIBLE
            // 차단 메뉴 활성화
            popupBind.layoutHorizBan.visibility = View.VISIBLE
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

        // 수정 메뉴 클릭 시
        popupBind.layoutHorizEdit.setOnClickListener {
            popupWindow.dismiss()
        }

        // 삭제 메뉴 클릭 시
        popupBind.layoutHorizDelete.setOnClickListener {
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
                Log.d(TAG, "요청 성공 시 받아온 게시물 제목 : ${responsePostDetail.title}")
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
     * @description - 받아온 상세 조회 정보를 바탕으로 뷰에 보여주기
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-18 | 2022-08-25
     */
    @SuppressLint("ResourceAsColor")
    private fun settingPostDetailView(responsePostDetail: ResponsePostDetail){
        // 툴 바 수정 버튼 뷰 처리
        if(responsePostDetail.user?.userId == ResponseExistLogin.baseUserInfo?.userId) // 게시글 작성자가 본인이라면 툴 바의 수정 버튼 활성화
            binding.postDetailBtnModify.visibility = View.VISIBLE
        else
            binding.postDetailBtnModify.visibility = View.GONE
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
     * @description - 해당 게시글의 상단 박스 영역의 제목, 위치, 시간, 가격, 주요 내용을 보여주는 함수 (게시글 주요 컨텐츠 담당 함수)
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
        // 게시글 작성 시간
        var formatter:DateTimeFormatter = if(Utils.checkDate(responsePostDetail.createdDate)) // 날짜 형식이 ssssss라면
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        else // 날짜 형식이 sssss라면
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSS")

        val localStartDateTime = LocalDateTime.parse(responsePostDetail.createdDate, formatter)
        binding.tvPostTime.text = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분").format(localStartDateTime)
    }

    /**
     * @description - 해당 게시글의 해시태그 리스트를 받아와 본문에서 해시태그를 강조하여 보여주는 함수 (게시글 해시태그 담당 함수)
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-25
     */
    private fun setViewHashTag(responsePostDetail: ResponsePostDetail){
        // 해시 태그 리스트 색상 표시
        Log.d(TAG, "해당 게시물의 해시 태그 리스트 : ${responsePostDetail.hashtagList}")
        Log.d(TAG, "해당 게시물의 전체 컨텐츠 : ${responsePostDetail.content}")
        var spannableString = SpannableString(responsePostDetail.content) // 텍스트 뷰의 특정 문자열 처리를 위한 spannableString 객체 생성
        var startList = ArrayList<Int>()
        for(hashString in responsePostDetail.hashtagList){
            Log.d(TAG, "해시 태그 값 : $hashString")
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
            adapter = ImageSliderAdapter(
                this,
                responsePostDetail.mediaList) // viewPager2 어댑터 세팅
            binding.viewPager.adapter = adapter
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
                finish()
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
        binding.tvPostContent.text = "테스트테스트"
    }
}