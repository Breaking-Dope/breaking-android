package com.dope.breaking

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.adapter.FeedAdapter
import com.dope.breaking.adapter.UserSearchAdapter
import com.dope.breaking.board.PostDetailActivity
import com.dope.breaking.databinding.ActivityFeedSearchBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseMainFeed
import com.dope.breaking.model.response.ResponseUserSearch
import com.dope.breaking.post.PostManager
import com.dope.breaking.user.UserProfile
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedSearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedSearchBinding
    private var searchState = 0 // 검색 카테고리
    private val postManager = PostManager()
    private lateinit var feedAdapter: FeedAdapter // 피드 리스트 어댑터
    private lateinit var userAdapter: UserSearchAdapter // 유저 검색 리스트 어댑터
    private lateinit var filterDialog: DialogUtil.FilterOptionDialog // 필터 dialog
    private lateinit var sortDialog: DialogUtil.SortOptionDialog // 정렬 dialog
    private var searchContent: String = "" // 문자열 검색인 경우 검색 키워드
    private var hashtagContent: String = "" // 해시태그 검색인 경우 검색 키워드
    private var userContent: String = "" // 유저 검색인 경우 검색 키워드
    private val feedList = mutableListOf<ResponseMainFeed?>() // 피드 리스트
    private val userList = mutableListOf<ResponseUserSearch?>() // 유저 검색 리스트
    private var isObtainedAll = false // 모두 다 받아와졌는지 판단
    private var isLoading = false // 로딩 중 판단

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedSearchBinding.inflate(layoutInflater)

        feedAdapter = FeedAdapter(this, feedList) // 피드 어댑터
        // 피드 아이템 클릭 이벤트
        feedAdapter.setItemListClickListener(object : FeedAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int) {
                moveToPostDetailPage(position) // 세부 조회 페이지로 이동
            }
        })
        userAdapter = UserSearchAdapter(this, userList) // 유저 검색 어댑터

        // 리사이클러뷰 divider 지정
        binding.rcvSearchList.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayout.VERTICAL
            )
        )

        initHashtagExplainSpannable()
        initUserExplainSpannable()
        // 요청 토큰
        val token = ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(this).getAccessTokenFromLocal()

        // 검색창 입력 감지
        val textWatcher = object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0!!.isNotEmpty()) { // 입력내용이 비어있지 않다면
                    if (p0[0] == '#') { // 첫 글자가 # 이라면
                        searchState = 1 // 해시태그 검색 처리
                        binding.btnMainFilter.isEnabled = true
                        binding.btnMainSort.isEnabled = true
                        binding.etSearchBar.setTextColor(getColor(R.color.sign_up_user_type_text_color))
                        binding.etSearchBar.setBackgroundResource(R.drawable.hashtag_search_input_background)
                    } else if (p0[0] == '@') { // 첫 글자가 @ 이라면
                        searchState = 2 // 유저 검색 처리
                        binding.btnMainFilter.isEnabled = false
                        binding.btnMainSort.isEnabled = false
                        binding.etSearchBar.setTextColor(getColor(R.color.teal_700))
                        binding.etSearchBar.setBackgroundResource(R.drawable.user_search_input_background)
                    } else { // 둘다 아니라면
                        searchState = 0 // 일반 게시글 검색 처리
                        binding.btnMainFilter.isEnabled = true
                        binding.btnMainSort.isEnabled = true
                        binding.etSearchBar.setTextColor(getColor(R.color.black))
                        binding.etSearchBar.setBackgroundResource(R.drawable.input_background)
                    }
                } else {
                    searchState = 0
                    binding.btnMainFilter.isEnabled = true
                    binding.btnMainSort.isEnabled = true
                    binding.etSearchBar.setBackgroundResource(R.drawable.input_background)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {

            }
        }

        // 뒤로가기 버튼 클릭 이벤트
        binding.ibBackButton.setOnClickListener {
            finish()
        }

        // 필터 옵션 다이얼로그
        filterDialog = DialogUtil().FilterOptionDialog(this) {
            searchFeed(0, token) // 현재 카테고리에 따른 검색 시작
        }

        // 정렬 옵션 다이얼로그
        sortDialog = DialogUtil().SortOptionDialog(this) {
            binding.btnMainSort.text =
                getString(ValueUtil.SORT_OPTIONS_VIEW[sortDialog.sortIndex]) // 버튼 텍스트 변경
            searchFeed(0, token) // 현재 카테고리에 따른 검색 시작
        }
        // 정렬 옵션 버튼 클릭 이벤트
        binding.btnMainSort.setOnClickListener {
            sortDialog.show() // 다이얼로그 실행
        }

        // 필터 옵션 버튼 클릭 이벤트
        binding.btnMainFilter.setOnClickListener {
            filterDialog.show() // 다이얼로그 실행
        }

        binding.etSearchBar.addTextChangedListener(textWatcher) // 텍스트 감지 리스너 등록

        // 키보드에서 검색 버튼 클릭 시 이벤트
        binding.etSearchBar.setOnKeyListener { view, i, keyEvent ->
            // 클릭했다면
            if ((keyEvent.action == KeyEvent.ACTION_DOWN) && i == KeyEvent.KEYCODE_ENTER) {
                closeKeyboard(view) // 키보드 닫기
                searchFeed(0, token) // 검색 시작
                true
            } else {
                false
            }
        }

        // 리사이클러뷰 스크롤 이벤트 등록
        binding.rcvSearchList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // 리스트의 마지막 인덱스 구하기
                val lastIndex =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                // 가져온 아이템 개수가 가져와야할 아이템 개수보다 적을 경우 스크롤 이벤트 무시
                if (recyclerView.adapter!!.itemCount < ValueUtil.FEED_SIZE) {
                    return
                }

                if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainedAll && !isLoading) {
                    // 커서 id는 검색 카테고리에 따라 다르게 할당 (피드: postId, 유저: userId)
                    val currentCursor = if (searchState != 2) {
                        feedAdapter.addItem(null)
                        feedList[lastIndex]!!.postId
                    } else {
                        userAdapter.addItem(null)
                        userList[lastIndex]!!.userId.toInt()
                    }

                    // 리스트에 새로운 내용을 append 하도록 하는 검색 요청
                    searchFeed(currentCursor, token, {
                        if (searchState != 2) { // 게시글 검색이라면
                            val list = it as List<ResponseMainFeed> // 데이터 타입 변환
                            if (list.size < ValueUtil.FEED_SIZE) { // 정량으로 가져오는 개수보다 적다면
                                feedAdapter.removeLast() // 로딩 아이템 제거
                                isObtainedAll = true // 더 이상 받아올 피드가 없다는 상태로 전환
                            } else { // 리스트가 있다면
                                feedAdapter.removeLast() // 먼저 로딩 아이템 제거
                            }
                        } else { // 유저 검색이라면
                            val list = it as List<ResponseUserSearch> // 데이터 타입 변환
                            if (list.size < ValueUtil.FEED_SIZE) { // 정량으로 가져오는 개수보다 적다면
                                userAdapter.removeLast() // 로딩 아이템 제거
                                isObtainedAll = true // 더 이상 받아올 피드가 없다는 상태로 전환
                            } else { // 리스트가 있다면
                                userAdapter.removeLast() // 먼저 로딩 아이템 제거
                            }
                        }
                    }, true)
                }
            }
        })
        setContentView(binding.root)
    }

    /**
     * 키보드를 닫는 함수
     * @param view(View): EditText
     * @author Seunggun Sin
     * @since 2022-08-30
     */
    private fun closeKeyboard(view: View) {
        val inputManager =
            this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(view.windowToken, 0)
    }


    /**
     * 피드 문자열 검색을 요청하는 프로세스 함수
     * @param cursorId(Int): 마지막으로 요청한 리스트의 마지막 인덱스의 게시글 아이디
     * @param token(String): 본인의 Jwt 토큰
     * @param init(() -> Unit): 요청 전 함수
     * @param last((List<ResponseMainFeed>) -> Unit): 요청 후 함수
     * @param error((ResponseErrorException) -> Unit): 에러 발생 시 함수
     * @author Seunggun Sin
     * @since 2022-08-30
     */
    private fun processStringSearchFeed(
        cursorId: Int,
        token: String,
        init: () -> Unit,
        last: (List<ResponseMainFeed>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기 함수 호출
            searchContent = binding.etSearchBar.text.toString() // 문자열 검색 키워드에 입력한 값 저장
            try {
                val responseList = postManager.startSearchStringFeed(
                    cursorId,
                    ValueUtil.FEED_SIZE,
                    searchContent,
                    sortDialog.sortIndex,
                    filterDialog.sellState,
                    filterDialog.startDate,
                    filterDialog.endDate,
                    filterDialog.lastMin,
                    token
                ) // 문자열 검색 요청하여 리스트 받아오기
                last(responseList) // 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e) // 에러 함수 호출
            }
        }
    }

    /**
     * 피드 해시태그 검색을 요청하는 프로세스 함수
     * @param cursorId(Int): 마지막으로 요청한 리스트의 마지막 인덱스의 게시글 아이디
     * @param token(String): 본인의 Jwt 토큰
     * @param init(() -> Unit): 요청 전 함수
     * @param last((List<ResponseMainFeed>) -> Unit): 요청 후 함수
     * @param error((ResponseErrorException) -> Unit): 에러 발생 시 함수
     * @author Seunggun Sin
     * @since 2022-08-30
     */
    private fun processHashtagSearchFeed(
        cursorId: Int,
        token: String,
        init: () -> Unit,
        last: (List<ResponseMainFeed>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기 함수 호출
            hashtagContent = binding.etSearchBar.text.toString() // 해시태그 키워드에 입력한 값 저장
            try {
                val responseList = postManager.startSearchHashtagFeed(
                    cursorId,
                    ValueUtil.FEED_SIZE,
                    hashtagContent,
                    sortDialog.sortIndex,
                    filterDialog.sellState,
                    filterDialog.startDate,
                    filterDialog.endDate,
                    filterDialog.lastMin,
                    token
                ) // 해시태그 검색 요청하여 리스트 가져오기
                last(responseList) // 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e) // 에러 함수 호출
            }
        }
    }

    /**
     * 유저 검색을 요청하는 프로세스 함수
     * @param cursorId(Int): 마지막으로 요청한 리스트의 마지막 인덱스의 유저 아이디
     * @param token(String): 본인의 Jwt 토큰
     * @param init(() -> Unit): 요청 전 함수
     * @param last((List<ResponseUserSearch>) -> Unit): 요청 후 함수
     * @param error((ResponseErrorException) -> Unit): 에러 발생 시 함수
     * @author Seunggun Sin
     * @since 2022-08-30
     */
    private fun processUserSearch(
        cursorId: Int,
        token: String,
        init: () -> Unit,
        last: (List<ResponseUserSearch>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기 함수 호출
            userContent = binding.etSearchBar.text.toString() // 유저 키워드에 입력한 값 저장
            try {
                val responseList = UserProfile(this@FeedSearchActivity)
                    .startGetUserSearch(
                        cursorId, ValueUtil.FEED_SIZE, userContent, token
                    ) // 유저 검색 요청하여 리스트 가져오기
                last(responseList) // 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e) // 에러 함수 호출
            }
        }
    }

    /**
     * 검색 카테고리에 따라 검색 요청을 하고, view 처리 및 리스트 처리를 해주는 묶음 함수
     * @param currentCursor(Int): 현재 커서 id
     * @param token(String): Jwt 토큰
     * @param init((Any) -> Unit): 응답받고 난 뒤, 호출하여 각 상황에 맞게 처리하는 커스텀 함수
     * @param append(Boolean): 리스트를 덧붙이는 것인지, 초기화하는 것인지 구분
     * @author Seunggun Sin
     * @since 2022-08-30 | 2022-09-01
     */
    private fun searchFeed(
        currentCursor: Int,
        token: String,
        init: (Any) -> Unit = {},
        append: Boolean = false
    ) {
        if (!append) // 초기화하는 상태라면
            isObtainedAll = false // 더 이상 받을 것이 없는 상태 초기화

        // 검색 카테고리 케이스 구분
        when (searchState) {
            0 -> { // 일반 게시글 검색
                if (binding.etSearchBar.text.toString().isNotEmpty()) // 입력 값이 있다면
                // 문자열 검색 요청
                    processStringSearchFeed(currentCursor, token, {
                        if (!append) { // 초기화하는 상태, 즉, 새로 받아오는 요청인 경우
                            // 로딩 처리
                            binding.progressSearch.visibility = View.VISIBLE
                            binding.rcvSearchList.visibility = View.GONE
                        }
                    }, {
                        init(it) // 스크롤 이벤트에 쓰이는 함수 호출
                        if (append) { // 덧붙이는 것이라면
                            feedAdapter.addItems(it) // 그대로 리스트에 추가
                        } else { // 새로 요청 받는 것이라면
                            binding.rcvSearchList.adapter = feedAdapter // 어댑터 초기화
                            feedAdapter.replaceAll(it) // 아이템 대체
                            binding.progressSearch.visibility = View.GONE
                            binding.rcvSearchList.visibility = View.VISIBLE
                        }

                        // 나머지 view 처리들
                        binding.tvSearchResult.visibility = View.VISIBLE
                        binding.interDividerTop.visibility = View.VISIBLE
                        binding.interDividerBottom.visibility = View.VISIBLE

                        // 검색 결과에 대한 텍스트 처리
                        spanningSearchResult()

                        // 리스트가 비어있다면 비어있는 view 처리
                        if (!append) {
                            if (it.isEmpty())
                                binding.tvNoResult.visibility = View.VISIBLE
                            else
                                binding.tvNoResult.visibility = View.GONE
                        }

                    }, {
                        it.printStackTrace()
                    })
            }
            1 -> { // 해시태그 검색
                if (binding.etSearchBar.text.toString().length > 1) // 적어도 #을 포함하여 2글자 이상이여야 가능
                // 해시태그 검색 요청
                    processHashtagSearchFeed(currentCursor, token, {
                        if (!append) { // 새로 받아오는 경우
                            // 로딩 처리
                            binding.progressSearch.visibility = View.VISIBLE
                            binding.rcvSearchList.visibility = View.GONE
                        }
                    }, {
                        init(it) // 스크롤 이벤트 처리
                        if (append) { // 덧붙이는 것이라면
                            feedAdapter.addItems(it) // 리스트에 추가
                        } else { // 새로 요청하는 것이라면
                            binding.rcvSearchList.adapter = feedAdapter // 어댑터 초기화
                            feedAdapter.replaceAll(it) // 아이템 대체
                            binding.progressSearch.visibility = View.GONE
                            binding.rcvSearchList.visibility = View.VISIBLE
                        }

                        // 나머지 view 처리
                        binding.tvSearchResult.visibility = View.VISIBLE
                        binding.interDividerTop.visibility = View.VISIBLE
                        binding.interDividerBottom.visibility = View.VISIBLE

                        // 검색 결과에 대한 텍스트 처리
                        spanningSearchResult()

                        // 리스트가 비어있다면 비어있는 view 처리
                        if (!append) {
                            if (it.isEmpty())
                                binding.tvNoResult.visibility = View.VISIBLE
                            else
                                binding.tvNoResult.visibility = View.GONE
                        }

                    }, {
                        it.printStackTrace()
                    })
            }
            2 -> { // 유저 검색
                if (binding.etSearchBar.text.toString().length > 1) { // 적어도 @을 포함하여 2글자 이상이여야 가능
                    // 유저 검색 요청
                    processUserSearch(currentCursor, token, {
                        if (!append) { // 새로 받아오는 것이라면
                            // 로딩 처리
                            binding.progressSearch.visibility = View.VISIBLE
                            binding.rcvSearchList.visibility = View.GONE
                        }

                    }, {
                        init(it) // 스크롤 이벤트 처리
                        if (append) { // 덧붙이는 것이라면
                            userAdapter.addItems(it) // 리스트에 추가
                        } else {
                            binding.rcvSearchList.adapter = userAdapter // 어댑터 초기화
                            userAdapter.replaceAll(it) // 아이템 대체
                            binding.progressSearch.visibility = View.GONE
                            binding.rcvSearchList.visibility = View.VISIBLE
                        }

                        // 나머지 view 처리
                        binding.tvSearchResult.visibility = View.VISIBLE
                        binding.interDividerTop.visibility = View.VISIBLE
                        binding.interDividerBottom.visibility = View.VISIBLE

                        // 검색 결과에 대한 텍스트 처리
                        spanningSearchResult()

                        // 리스트가 비어있다면 비어있는 view 처리
                        if (!append) {
                            if (it.isEmpty())
                                binding.tvNoResult.visibility = View.VISIBLE
                            else
                                binding.tvNoResult.visibility = View.GONE
                        }
                    }, {
                        it.printStackTrace()
                    })
                }
            }
        }
    }

    /**
     * 검색 결과에 대한 텍스트 spanning 처리
     * @author Seunggun Sin
     * @since 2022-08-30
     */
    private fun spanningSearchResult() {
        var target = "" // 검색 카테고리
        var color = 0 // 색상 리소스 id
        var inter = "" // 카테고리에 따른 검색 결과 안내 텍스트
        when (searchState) {
            0 -> { // 게시글 검색
                target = searchContent
                color = R.color.breaking_color
                inter = ""
            }
            1 -> { // 해시태그 검색
                target = hashtagContent
                color = R.color.breaking_color
                inter = " 해시태그"
            }
            2 -> { // 유저 검색
                target = userContent
                color = R.color.teal_700
                inter = " 유저"
            }
        }
        val tmp = "\"$target\" $inter 검색 결과" // 최종 검색 결과 텍스트
        val spannableString = SpannableString(tmp)
        val index = tmp.indexOf(target) // 검색 키워드 시작 인덱스 추출

        spannableString.setSpan(
            ForegroundColorSpan(getColor(color)),
            index,
            index + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        ) // 검색 키워드만 지정한 색깔로 처리

        binding.tvSearchResult.text = spannableString
    }

    /**
     * 해시태그 검색 설명에 대한 초기 spannable 처리
     * @author Seunggun Sin
     * @since 2022-09-01
     */
    private fun initHashtagExplainSpannable() {
        val tmp = binding.tvAnnounceSearchHashtag.text.toString()
        val index = tmp.indexOf("#")
        val spannableString = SpannableString(tmp)

        // 색상 지정
        spannableString.setSpan(
            ForegroundColorSpan(getColor(R.color.breaking_color)),
            index,
            index + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 볼드 처리
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            index,
            index + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val target = "#해시태그"
        val index2 = tmp.indexOf(target)

        // 색상 지정
        spannableString.setSpan(
            ForegroundColorSpan(getColor(R.color.breaking_color)),
            index2,
            index2 + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 볼드 처리
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            index2,
            index2 + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvAnnounceSearchHashtag.text = spannableString
    }

    /**
     * 유저 검색 설명에 대한 초기 spannable 처리
     * @author Seunggun Sin
     * @since 2022-09-01
     */
    private fun initUserExplainSpannable() {
        val tmp = binding.tvAnnounceSearchUser.text.toString()
        val index = tmp.indexOf("@")
        val spannableString = SpannableString(tmp)

        // 색상 지정
        spannableString.setSpan(
            ForegroundColorSpan(getColor(R.color.teal_700)),
            index,
            index + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 볼드 처리
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            index,
            index + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val target = "@홍길동"
        val index2 = tmp.indexOf(target)

        // 색상 지정
        spannableString.setSpan(
            ForegroundColorSpan(getColor(R.color.teal_700)),
            index2,
            index2 + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 볼드 처리
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            index2,
            index2 + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvAnnounceSearchUser.text = spannableString
    }

    /**
     * 어댑터에서 아이템 리스트를 클릭했을 때, 해당 postId를 받아와 세부 조회 액티비티로 넘겨주는 함수
     * @param position(Int): 현재 리스트의 인덱스
     * @author Seunggun Sin
     * @since 2022-09-12
     */
    private fun moveToPostDetailPage(position: Int) {
        val intent = Intent(this, PostDetailActivity::class.java)
        intent.putExtra("postId", feedList[position]!!.postId)
        startActivity(intent)
    }
}