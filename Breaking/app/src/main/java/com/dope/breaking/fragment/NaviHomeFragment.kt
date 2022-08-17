package com.dope.breaking.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.adapter.FeedAdapter
import com.dope.breaking.board.PostActivity
import com.dope.breaking.databinding.FragmentNaviHomeBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseMainFeed
import com.dope.breaking.post.PostManager
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NaviHomeFragment : Fragment() {
    private var mbinding: FragmentNaviHomeBinding? = null // 바인딩 변수 초기화
    private val binding get() = mbinding!! // 바인딩 변수 재할당
    private lateinit var filterDialog: DialogUtil.FilterOptionDialog // 필터 dialog
    private lateinit var sortDialog: DialogUtil.SortOptionDialog // 정렬 dialog
    private val feedList = mutableListOf<ResponseMainFeed?>() // 피드 리스트
    private lateinit var adapter: FeedAdapter // 피드 리스트 어댑터
    private var isObtainedAll = false // 모든 피드를 받았는지 판단(더 이상 요청할 것이 없는)
    private var isLoading = false  // 로딩 중 판단
    private val postManager = PostManager() // 게시글 기능 관련 클래스 객체 생성

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mbinding = FragmentNaviHomeBinding.inflate(inflater, container, false)

        // 요청 Jwt 토큰 가져오기
        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(requireContext()).getTokenFromLocal()

        // 피드 요청 에러 시 띄워줄 다이얼로그 정의
        val requestErrorDialog =
            DialogUtil().SingleDialog(requireContext(), "피드를 가져오는데 문제가 발생하였습니다.", "확인")

        // 필터 다이얼로그 객체 초기화 및 버튼 클릭 이벤트 정의
        filterDialog = DialogUtil().FilterOptionDialog(requireContext()) {
            processGetMainFeed(0, token, {
                // 로딩 시작
                binding.progressbarLoading.visibility = View.VISIBLE
                binding.rcvMainFeed.visibility = View.GONE

                // 리스트 비우기
                adapter.clearList()
                isObtainedAll = false // 더 이상 얻을 피드가 없는 상태 초기화
            }, {
                // 로딩 종료
                binding.rcvMainFeed.visibility = View.VISIBLE
                binding.progressbarLoading.visibility = View.GONE

                if (it.isEmpty()) { // 리스트가 비어있다면
                    // 비어있다면 화면 뿌려주기
                    binding.tvNoFeedAlert.visibility = View.VISIBLE
                    binding.rcvMainFeed.visibility = View.GONE
                } else { // 아니라면
                    // 리스트 보여주기
                    binding.tvNoFeedAlert.visibility = View.GONE
                    binding.rcvMainFeed.visibility = View.VISIBLE
                }
                adapter.addItems(it) // 받아온 리스트 추가하기
            }, {
                it.printStackTrace()
                requestErrorDialog.show()
            })
        }

        // 정렬 다이얼로그 객체 초기화 및 버튼 클릭 이벤트 정의
        sortDialog = DialogUtil().SortOptionDialog(requireContext()) {
            processGetMainFeed(0, token, {
                // 정렬 옵션에서 선택한 값을 버튼 텍스트에 보여주기
                binding.btnMainSort.text =
                    getString(ValueUtil.SORT_OPTIONS_VIEW[sortDialog.sortIndex])

                // 로딩 시작
                binding.progressbarLoading.visibility = View.VISIBLE
                binding.rcvMainFeed.visibility = View.GONE

                // 리스트 비우기
                adapter.clearList()
                isObtainedAll = false // 더 이상 얻을 피드가 없는 상태 초기화
            }, {
                // 로딩 종료
                binding.rcvMainFeed.visibility = View.VISIBLE
                binding.progressbarLoading.visibility = View.GONE

                if (it.isEmpty()) { // 리스트가 비어있다면
                    // 비어있다면 화면 뿌려주기
                    binding.tvNoFeedAlert.visibility = View.VISIBLE
                    binding.rcvMainFeed.visibility = View.GONE
                } else { // 아니라면
                    // 리스트 보여주기
                    binding.tvNoFeedAlert.visibility = View.GONE
                    binding.rcvMainFeed.visibility = View.VISIBLE
                }
                adapter.addItems(it) // 받아온 리스트 추가하기
            }, {
                it.printStackTrace()
                requestErrorDialog.show()
            })
        }

        // 정렬 옵션 버튼 클릭 이벤트
        binding.btnMainSort.setOnClickListener {
            sortDialog.show() // 다이얼로그 실행
        }

        // 필터 옵션 버튼 클릭 이벤트
        binding.btnMainFilter.setOnClickListener {
            filterDialog.show() // 다이얼로그 실행
        }

        /*
         최초로 피드 가져오는 요청
         */
        processGetMainFeed(0, token, {
            // 로딩 시작
            binding.progressbarLoading.visibility = View.VISIBLE
            binding.rcvMainFeed.visibility = View.GONE

            isObtainedAll = false
        }, { it ->
            feedList.addAll(it) // 동적 리스트에 가져온 리스트 추가
            adapter = FeedAdapter(requireContext(), feedList) // 어댑터 초기화

            // 리스트의 divider 선 추가
            binding.rcvMainFeed.addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    LinearLayout.VERTICAL
                )
            )
            // 로딩 종료
            binding.rcvMainFeed.visibility = View.VISIBLE
            binding.progressbarLoading.visibility = View.GONE

            // 어댑터 지정
            binding.rcvMainFeed.adapter = adapter

            /*
                RecyclerView 스크롤 이벤트 리스너 정의
             */
            binding.rcvMainFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    // 스크롤하면서 리스트의 가장 마지막 위치에 도달했을 때, 그 인덱스 값 가져오기
                    val lastIndex =
                        (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                    // 실제 데이터 리스트의 마지막 인덱스와 스크롤 이벤트에 의한 인덱스 값이 같으면서
                    // 스크롤이 드래깅 중이면서
                    // 피드 요청이 더 가능하면서
                    // 로딩 중이 아니라면
                    if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainedAll && !isLoading) {
                        processGetMainFeed(feedList[lastIndex]!!.postId, token, {
                            adapter.addItem(null) // 로딩 창 아이템 추가
                            isLoading = true // 로딩 시작 상태 전환
                        }, {
                            if (it.isEmpty()) { // 리스트가 비어있다면
                                isObtainedAll = true // 더 이상 받아올 피드가 없다는 상태로 전환
                                adapter.removeLast() // 로딩 아이템 제거
                                isLoading = false
                            } else { // 리스트가 있다면
                                adapter.removeLast() // 먼저 로딩 아이템 제거
                                adapter.addItems(it) // 받아온 리스트 추가
                                isLoading = false // 로딩 종료
                            }
                        }, {
                            // BSE450 에러의 경우 더 이상 제보가 없는 경우 발생
                            if (it.message!!.contains("BSE450")) {
                                isObtainedAll = true
                                adapter.removeLast()
                            }
                        })
                    }
                }
            })
        }, {
            it.printStackTrace()
            requestErrorDialog.show()
        })

        // 제보하기 버튼을 누르면 게시글 작성 페이지로 이동
        binding.fabPosting.setOnClickListener {
            val intent = Intent(activity, PostActivity::class.java)
            startActivity(intent)
        }
        return binding.root
    }

    /**
     * 메인 피드 프로세스 함수
     * @param postId(Int): 다음 요청을 하고자 하는 이전에 획득한 마지막 postId
     * @param token(String): Jwt 토큰
     * @param init(() -> Unit): 요청 전처리에 필요한 동작에 대한 함수
     * @param last((List<ResponseMainFeed>) -> Unit): 요청 후처리에 필요한 동작에 대한 함수(parameter: 얻은 list)
     * @param error((ResponseErrorException) -> Unit): 요청 에러 시 발생하는 예외처리를 해주는 함수
     * @author Seunggun Sin
     * @since 2022-08-16
     */
    private fun processGetMainFeed(
        postId: Int,
        token: String,
        init: () -> Unit,
        last: (List<ResponseMainFeed>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init() // 전 처리 함수 호출
            try {
                val responseList = postManager.startGetMainFeed(
                    postId,
                    ValueUtil.FEED_SIZE,
                    sortDialog.sortIndex,
                    filterDialog.sellState,
                    filterDialog.startDate,
                    filterDialog.endDate,
                    filterDialog.lastMin,
                    token
                ) // 요청을 통해 리스트 받아오기

                last(responseList) // 받아온 리스트를 바탕으로 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e) // 예외 발생 시 에러 함수 호출
            }
        }
    }
}