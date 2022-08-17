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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mbinding = FragmentNaviHomeBinding.inflate(inflater, container, false)
        val postManager = PostManager() // 게시글 기능 관련 클래스 객체 생성

        // 필터 다이얼로그 객체 초기화 및 버튼 클릭 이벤트 정의
        filterDialog = DialogUtil().FilterOptionDialog(requireContext()) {
            // 로딩 시작
            binding.progressbarLoading.visibility = View.VISIBLE
            binding.rcvMainFeed.visibility = View.GONE

            // 리스트 비우기
            adapter.clearList()
            isObtainedAll = false // 더 이상 얻을 피드가 없는 상태 초기화

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val token =
                        ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(requireContext()).getAccessTokenFromLocal() // 토큰 가져오기

                    val list = postManager.startGetMainFeed(
                        0, // 마지막으로 받은 postId (최초 호출 시, 0 또는 null 로 지정)
                        ValueUtil.FEED_SIZE, // 가져올 피드 개수 (현재 10개)
                        sortDialog.sortIndex, // 정렬 옵션 라디오 인덱스
                        filterDialog.sellState, // 필터 옵션, 판매 제보 라디오 인덱스
                        filterDialog.startDate, // 필터 옵션, 기간에서 시작 날짜
                        filterDialog.endDate, // 필터 옵션, 기간에서 종료 날짜
                        filterDialog.lastMin, // 필터 옵션, 최근 N분
                        token // 토큰
                    ) // 메인 피드 요청을 통해 응답 리스트 가져오기

                    // 로딩 종료
                    binding.rcvMainFeed.visibility = View.VISIBLE
                    binding.progressbarLoading.visibility = View.GONE

                    if (list.isEmpty()) { // 리스트가 비어있다면
                        // 비어있다면 화면 뿌려주기
                        binding.tvNoFeedAlert.visibility = View.VISIBLE
                        binding.rcvMainFeed.visibility = View.GONE
                    } else { // 아니라면
                        // 리스트 보여주기
                        binding.tvNoFeedAlert.visibility = View.GONE
                        binding.rcvMainFeed.visibility = View.VISIBLE
                    }
                    adapter.addItems(list) // 받아온 리스트 추가하기
                } catch (e: ResponseErrorException) { // 응답 에러
                    e.printStackTrace()
                }
            }

        }
        // 정렬 다이얼로그 객체 초기화 및 버튼 클릭 이벤트 정의
        sortDialog = DialogUtil().SortOptionDialog(requireContext()) {
            binding.btnMainSort.text =
                getString(ValueUtil.SORT_OPTIONS_VIEW[sortDialog.sortIndex]) // 선택한 값을 버튼 텍스트에 보여주기
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
        CoroutineScope(Dispatchers.Main).launch {
            // 로딩 시작
            binding.progressbarLoading.visibility = View.VISIBLE
            binding.rcvMainFeed.visibility = View.GONE

            isObtainedAll = false

            try {
                val token =
                    ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(requireContext()).getAccessTokenFromLocal()
                val list = postManager.startGetMainFeed(
                    0,
                    ValueUtil.FEED_SIZE,
                    sortDialog.sortIndex,
                    filterDialog.sellState,
                    filterDialog.startDate,
                    filterDialog.endDate,
                    filterDialog.lastMin,
                    token
                ) // 리스트 받아오기

                feedList.addAll(list) // 동적 리스트에 가져온 리스트 추가
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
                            CoroutineScope(Dispatchers.Main).launch {
                                adapter.addItem(null) // 로딩 창 아이템 추가
                                isLoading = true // 로딩 시작 상태 전환
                                try {
                                    val newList = postManager.startGetMainFeed(
                                        feedList[lastIndex]!!.postId, // 마지막 인덱스에 해당하는 postId (cursor)
                                        ValueUtil.FEED_SIZE,
                                        sortDialog.sortIndex,
                                        filterDialog.sellState,
                                        filterDialog.startDate,
                                        filterDialog.endDate,
                                        filterDialog.lastMin,
                                        token
                                    ) // 새로 받아온 리스트

                                    if (newList.isEmpty()) { // 리스트가 비어있다면
                                        isObtainedAll = true // 더 이상 받아올 피드가 없다는 상태로 전환
                                        adapter.removeLast() // 로딩 아이템 제거
                                    } else { // 리스트가 있다면
                                        adapter.removeLast() // 먼저 로딩 아이템 제거
                                        adapter.addItems(newList) // 받아온 리스트 추가
                                        isLoading = false // 로딩 종료
                                    }
                                } catch (e: ResponseErrorException) {
                                    // BSE450 에러의 경우 더 이상 제보가 없는 경우 발생
                                    if (e.message!!.contains("BSE450")) {
                                        isObtainedAll = true
                                        adapter.removeLast()
                                    }
                                }
                            }
                        }
                    }
                })
            } catch (e: ResponseErrorException) {
                e.printStackTrace()
            }
        }
        // 제보하기 버튼을 누르면 게시글 작성 페이지로 이동
        binding.fabPosting.setOnClickListener {
            val intent = Intent(activity, PostActivity::class.java)
            startActivity(intent)
        }
        return binding.root
    }
}