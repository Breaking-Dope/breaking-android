package com.dope.breaking.fragment.user_tab

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.R
import com.dope.breaking.adapter.FeedAdapter
import com.dope.breaking.board.PostDetailActivity
import com.dope.breaking.databinding.FragmentPurchasedTabBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseMainFeed
import com.dope.breaking.post.PostManager
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PurchasedTabFragment : Fragment() {
    private lateinit var binding: FragmentPurchasedTabBinding
    private val userFeedMutableList = mutableListOf<ResponseMainFeed?>()
    private val requestOption = "buy" // 현재 Fragment 의 옵션 - 구매한 제보글
    private var isLoading = false // 로딩 중 판단
    private var isObtainAll = false // 모든 피드를 받았는지 판단(더 이상 요청할 것이 없는)
    private var optionState = 0 // 판매 옵션 선택에 따른 상태
    private val postManager = PostManager() // 게시글 기능 클래스 객체
    private lateinit var adapter: FeedAdapter // 피드 어댑터

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPurchasedTabBinding.inflate(inflater, container, false)
        val userId = arguments?.getLong("userId") // 로딩 Fragment 로부터 받아온 유저 객체
        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(requireContext()).getAccessTokenFromLocal()

        // 리사이클러 뷰 divider
        binding.rcvUserPurchased.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayout.VERTICAL
            )
        )

        /*
            최초로 "전체" 옵션으로 유저 페이지 피드를 가져오는 요청
         */
        processGetUserFeed(userId!!, 0, ValueUtil.FILTER_SELL_OPTIONS[0], token, {
            showSkeletonView() // 스켈레톤 UI 표시
            isLoading = true // 로딩 시작 상태
        }, { it ->
            adapter = FeedAdapter(requireContext(), userFeedMutableList) // 우선 어댑터 초기화
            adapter.setItemListClickListener(object : FeedAdapter.OnItemClickListener {
                override fun onClick(v: View, position: Int) {
                    moveToPostDetailPage(position)
                }
            })
            binding.rcvUserPurchased.adapter = adapter // 리사이클러뷰에 적용

            if (it.isEmpty()) // 가져온 리스트가 비어있다면
                binding.tvEmptyText.visibility = View.VISIBLE // 비어있는 처리
            else
                binding.tvEmptyText.visibility = View.GONE // 리스트 존재 처리
            adapter.replaceAll(it) // 어댑터에 가져온 리스트 적용하기

            /*
                스크롤 이벤트
             */
            binding.rcvUserPurchased.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    // 리스트의 마지막 인덱스
                    val lastIndex =
                        (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                    // 가져온 아이템 사이즈가 가져와야하는 사이즈보다 작은 경우 새로운 요청을 못하게 막기
                    if (recyclerView.adapter!!.itemCount < ValueUtil.USER_FEED_SIZE) {
                        return
                    }

                    // 실제 데이터 리스트의 마지막 인덱스와 스크롤 이벤트에 의한 인덱스 값이 같으면서
                    // 스크롤이 드래깅 중이면서
                    // 피드 요청이 더 가능하면서
                    // 로딩 중이 아니라면
                    if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainAll && !isLoading) {
                        processGetUserFeed(
                            userId, // 가져올 대상의 유저 id
                            userFeedMutableList[lastIndex]!!.postId, // 다음 요청에 필요한 마지막 게시글 id
                            ValueUtil.FILTER_SELL_OPTIONS[optionState], // 판매 상태 옵션
                            token, // 토큰
                            {
                                adapter.addItem(null) // 로딩 아이템 추가
                                isLoading = true // 로딩 시작 중
                            },
                            {
                                if (it.size < ValueUtil.USER_FEED_SIZE) { // 리스트 크기가 가져오는 아이템보다 적은 경우
                                    adapter.removeLast() // 로딩 아이템 제거
                                    if (it.isNotEmpty()) // 가져온 리스트가 0이 아니라면
                                        adapter.addItems(it) // 나머지 리스트 아이템 추가
                                    isObtainAll = true // 더 이상 가져올 것이 없는 상태 전환
                                } else { // 충분히 가져온다면
                                    adapter.removeLast() // 로딩 아이템 제거
                                    adapter.addItems(it) // 리스트 아이템 추가
                                }
                                isLoading = false // 로딩 상태 해제
                            },
                            {
                                if (it.message!!.contains("BSE450")) {
                                    isObtainAll = true
                                    adapter.removeLast()
                                }
                            })
                    }
                }
            })
            dismissSkeletonView() // 스켈레톤 UI 해제
            isLoading = false // 로딩 상태 해제
        }, {
            // 에러
        })

        // 전체 옵션 선택 시
        binding.tvOptionAll.setOnClickListener {
            if (!isLoading) { // 로딩 중이 아닐 때 가능
                optionState = 0 // 상태 변경
                isObtainAll = false
                processGetUserFeed(userId, 0, ValueUtil.FILTER_SELL_OPTIONS[optionState], token, {
                    isLoading = true // 로딩 상태 전환
                    selectOption(binding.tvOptionAll) // 옵션 선택 view 전환
                    unselectOption(binding.tvOptionSold) // 옵션 미선택 view 전환
                    unselectOption(binding.tvOptionUnsold) // 옵션 미선택 view 전환
                    showSkeletonView() // 스켈레톤 UI 표시
                }, {
                    if (it.isEmpty()) // 리스트가 비어있다면
                        binding.tvEmptyText.visibility = View.VISIBLE // 비어있는 처리
                    else
                        binding.tvEmptyText.visibility = View.GONE // 존재하는 존리
                    adapter.replaceAll(it) // 리스트 아이템에 추가
                    dismissSkeletonView() // 스켈레톤 UI 해제
                    isLoading = false // 로딩 상태 해제
                }, {
                    // 에러
                })
            }
        }

        // 판매 옵션 선택 시
        binding.tvOptionSold.setOnClickListener {
            if (!isLoading) {
                optionState = 1
                isObtainAll = false
                processGetUserFeed(userId, 0, ValueUtil.FILTER_SELL_OPTIONS[optionState], token, {
                    isLoading = true
                    selectOption(binding.tvOptionSold)
                    unselectOption(binding.tvOptionAll)
                    unselectOption(binding.tvOptionUnsold)
                    showSkeletonView()
                }, {
                    if (it.isEmpty())
                        binding.tvEmptyText.visibility = View.VISIBLE
                    else
                        binding.tvEmptyText.visibility = View.GONE
                    adapter.replaceAll(it)
                    dismissSkeletonView()
                    isLoading = false
                }, {})
            }
        }

        // 미판매 옵션 선택 시
        binding.tvOptionUnsold.setOnClickListener {
            if (!isLoading) {
                optionState = 2
                isObtainAll = false
                processGetUserFeed(userId, 0, ValueUtil.FILTER_SELL_OPTIONS[optionState], token, {
                    isLoading = true
                    selectOption(binding.tvOptionUnsold)
                    unselectOption(binding.tvOptionSold)
                    unselectOption(binding.tvOptionAll)
                    showSkeletonView()
                }, {
                    if (it.isEmpty())
                        binding.tvEmptyText.visibility = View.VISIBLE
                    else
                        binding.tvEmptyText.visibility = View.GONE
                    adapter.replaceAll(it)
                    dismissSkeletonView()
                    isLoading = false
                }, {})
            }
        }
        return binding.root
    }

    /**
     * 스켈레톤 UI를 보여주는 함수 with shimmer effect
     * @author Seunggun Sin
     * @since 2022-08-19
     */
    private fun showSkeletonView() {
        binding.rcvUserPurchased.visibility = View.GONE
        binding.sflPostTabSkeleton.visibility = View.VISIBLE
        binding.sflPostTabSkeleton.startShimmer()
    }

    /**
     * 스켈레톤 UI를 종료하는 함수
     * @author Seunggun Sin
     * @since 2022-08-19
     */
    private fun dismissSkeletonView() {
        binding.sflPostTabSkeleton.stopShimmer()
        binding.sflPostTabSkeleton.visibility = View.GONE
        binding.rcvUserPurchased.visibility = View.VISIBLE
    }

    /**
     * 선택한 옵션에 대한 ui 변경
     * @param view(TextView): 선택된 텍스트
     * @author Seunggun Sin
     * @since 2022-08-19
     */
    private fun selectOption(view: TextView) {
        view.setTextColor(requireContext().getColor(R.color.breaking_bottom_clicked_color))
        view.typeface = Typeface.DEFAULT_BOLD
    }

    /**
     * 선택되지 않은 옵션에 대한 ui 변경
     * @param view(TextView): 선택되지 않은 텍스트
     * @author Seunggun Sin
     * @since 2022-08-19
     */
    private fun unselectOption(view: TextView) {
        view.setTextColor(requireContext().getColor(R.color.black))
        view.typeface = Typeface.DEFAULT
    }

    /**
     * 유저 페이지 피드 가져오는 프로세스 함수
     * @param userId(Long): 대상 유저 id
     * @param cursorId(Int): 마지막으로 요청한 게시글 id
     * @param sellOption(String): 판매 옵션
     * @param token(String): 본인 Jwt 토큰
     * @param init(() -> Unit): 요청 전 함수
     * @param last((List<ResponseMainFeed>) -> Unit): 요청 후 함수
     * @param error((ResponseErrorException) -> Unit): 에러 발생 시 함수
     */
    private fun processGetUserFeed(
        userId: Long,
        cursorId: Int,
        sellOption: String,
        token: String,
        init: () -> Unit,
        last: (List<ResponseMainFeed>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기 함수 호출
            try {
                val response = postManager.startGetUserPageFeed(
                    userId,
                    cursorId,
                    ValueUtil.USER_FEED_SIZE,
                    requestOption,
                    sellOption,
                    token
                ) // 유저에 대한 피드 요청
                last(response) // 후 처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e) // 에러 함수 호출
            }
        }
    }

    /**
     * 어댑터에서 아이템 리스트를 클릭했을 때, 해당 postId를 받아와 세부 조회 액티비티로 넘겨주는 함수
     * @param position(Int): 현재 리스트의 인덱스
     * @author Tae hyun Park | Seunggun Sin
     * @since 2022-08-18 | 2022-08-25
     */
    private fun moveToPostDetailPage(position: Int) {
        val intent = Intent(context, PostDetailActivity::class.java)
        intent.putExtra("postId", userFeedMutableList[position]!!.postId)
        startActivity(intent)
    }
}