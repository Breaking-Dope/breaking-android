package com.dope.breaking.fragment

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.adapter.FeedAdapter
import com.dope.breaking.board.PostActivity
import com.dope.breaking.board.PostDetailActivity
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
    private val TAG = "NaviHomeFragment.kt"
    private var mbinding: FragmentNaviHomeBinding? = null // 바인딩 변수 초기화
    private val binding get() = mbinding!! // 바인딩 변수 재할당
    private lateinit var filterDialog: DialogUtil.FilterOptionDialog // 필터 dialog
    private lateinit var sortDialog: DialogUtil.SortOptionDialog // 정렬 dialog
    private var feedList = mutableListOf<ResponseMainFeed?>() // 피드 리스트
    private lateinit var adapter: FeedAdapter // 피드 리스트 어댑터
    private var isObtainedAll = false // 모든 피드를 받았는지 판단(더 이상 요청할 것이 없는)
    private var isLoading = false  // 로딩 중 판단
    private val postManager = PostManager() // 게시글 기능 관련 클래스 객체 생성
    private var isWritePost = false // 제보하기 페이지에서 제보글 작성을 하고 돌아왔다면 true, 아니라면 false
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>> // 위치 권한에 관한 콜백 함수 정의
    private lateinit var isWriteActivityResult: ActivityResultLauncher<Intent> // 게시글 작성을 하고 돌아온다면 그 후 처리를 위한 activityResult

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mbinding = FragmentNaviHomeBinding.inflate(inflater, container, false)
        // 요청 Jwt 토큰 가져오기
        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(requireContext()).getAccessTokenFromLocal()

        // 피드 요청 에러 시 띄워줄 다이얼로그 정의
        val requestErrorDialog =
            DialogUtil().SingleDialog(requireContext(), "피드를 가져오는데 문제가 발생하였습니다.", "확인")

        // 필터 다이얼로그 객체 초기화 및 버튼 클릭 이벤트 정의
        filterDialog = DialogUtil().FilterOptionDialog(requireContext()) {
            processGetMainFeed(0, token, {
                showSkeletonView() // 스켈레톤 UI 시작
                // 리스트 비우기
                adapter.clearList()
                isObtainedAll = false // 더 이상 얻을 피드가 없는 상태 초기화
            }, {

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
                dismissSkeletonView() // 스켈레톤 UI 종료
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
                showSkeletonView() // 스켈레톤 UI 시작

                // 리스트 비우기
                adapter.clearList()
                isObtainedAll = false // 더 이상 얻을 피드가 없는 상태 초기화
            }, {

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
                dismissSkeletonView() // 스켈레톤 UI 종료
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
            showSkeletonView() // 스켈레톤 UI 시작
            isObtainedAll = false
        }, { it ->
            feedList.addAll(it) // 동적 리스트에 가져온 리스트 추가
            adapter = FeedAdapter(requireContext(), feedList) // 어댑터 초기화
            adapter.setItemListClickListener(object : FeedAdapter.OnItemClickListener {
                override fun onClick(v: View, position: Int) {
                    moveToPostDetailPage(position)
                }
            })
            if (it.isEmpty()) { // 리스트가 비어있다면
                // 비어있다면 화면 뿌려주기
                binding.tvNoFeedAlert.visibility = View.VISIBLE
                binding.rcvMainFeed.visibility = View.GONE
            } else { // 아니라면
                // 리스트 보여주기
                binding.tvNoFeedAlert.visibility = View.GONE
                binding.rcvMainFeed.visibility = View.VISIBLE
            }
            dismissSkeletonView() // 스켈레톤 UI 종료

            // 리스트의 divider 선 추가
            binding.rcvMainFeed.addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    LinearLayout.VERTICAL
                )
            )

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

                    // 가져온 아이템 사이즈가 가져와야하는 사이즈보다 작은 경우 새로운 요청을 못하게 막기
                    if (recyclerView.adapter!!.itemCount < ValueUtil.FEED_SIZE) {
                        return
                    }

                    // 실제 데이터 리스트의 마지막 인덱스와 스크롤 이벤트에 의한 인덱스 값이 같으면서
                    // 스크롤이 드래깅 중이면서
                    // 피드 요청이 더 가능하면서
                    // 로딩 중이 아니라면
                    if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainedAll && !isLoading) {
                        processGetMainFeed(feedList[lastIndex]!!.postId, token, {
                            adapter.addItem(null) // 로딩 창 아이템 추가
                            isLoading = true // 로딩 시작 상태 전환
                        }, {
                            if (it.size < ValueUtil.FEED_SIZE) { // 정량으로 가져오는 개수보다 적다면
                                adapter.removeLast() // 로딩 아이템 제거
                                if (it.isNotEmpty()) // 리스트가 비어있지 않다면
                                    adapter.addItems(it) // 받아온 리스트 추가
                                isObtainedAll = true // 더 이상 받아올 피드가 없다는 상태로 전환
                            } else { // 리스트가 있다면
                                adapter.removeLast() // 먼저 로딩 아이템 제거
                                adapter.addItems(it) // 받아온 리스트 추가
                            }
                            isLoading = false // 로딩 종료 상태 전환

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

        // 위치 권한에 대한 콜백 핸들링
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (it.all { permission -> permission.value == true }) {
                // 권한 허용했다면 제보하기 페이지로 이동
                val intent = Intent(activity, PostActivity::class.java)
                isWriteActivityResult.launch(intent)
            } else {
                // 권한 허용 X의 경우
                Toast.makeText(requireContext(), "위치 권한 동의가 필요한 컨텐츠입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 제보하기 버튼을 누르면 권한 허락받고 게시글 작성 페이지로 이동
        binding.fabPosting.setOnClickListener {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // PostActivity 에서 제보글을 작성하고 다시 돌아온다면
        isWriteActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    isWritePost =
                        it.data?.getBooleanExtra(
                            "isWritePost",
                            false
                        ) == true // 제보글을 썼다면 true, 쓰지 않았다면 false
                    if (isWritePost) { // true 면 피드 다시 조회하여 refresh

                        processGetMainFeed(0, token, {
                            showSkeletonView() // 스켈레톤 UI 시작
                            adapter.clearList()
                            isObtainedAll = false
                        }, {
                            feedList.addAll(it) // 동적 리스트에 가져온 리스트 추가
                            adapter = FeedAdapter(requireContext(), feedList) // 어댑터 초기화

                            if (it.isEmpty()) { // 리스트가 비어있다면
                                // 비어있다면 화면 뿌려주기
                                binding.tvNoFeedAlert.visibility = View.VISIBLE
                                binding.rcvMainFeed.visibility = View.GONE
                            } else { // 아니라면
                                // 리스트 보여주기
                                binding.tvNoFeedAlert.visibility = View.GONE
                                binding.rcvMainFeed.visibility = View.VISIBLE
                            }
                            dismissSkeletonView() // 스켈레톤 UI 종료
                            // 다시 false 로 변경
                            isWritePost = false
                        }, {
                            it.printStackTrace()
                            requestErrorDialog.show()
                        })
                    }
                }
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

    /**
     * 스켈레톤 UI를 보여주는 함수 with shimmer effect
     * @author Seunggun Sin
     * @since 2022-08-27
     */
    private fun showSkeletonView() {
        binding.rcvMainFeed.visibility = View.GONE
        binding.sflPostListSkeleton.visibility = View.VISIBLE
        binding.sflPostListSkeleton.startShimmer()
    }

    /**
     * 스켈레톤 UI를 종료하는 함수
     * @author Seunggun Sin
     * @since 2022-08-27
     */
    private fun dismissSkeletonView() {
        binding.sflPostListSkeleton.stopShimmer()
        binding.sflPostListSkeleton.visibility = View.GONE
        binding.rcvMainFeed.visibility = View.VISIBLE
    }

    /**
     * 어댑터에서 아이템 리스트를 클릭했을 때, 해당 postId를 받아와 세부 조회 액티비티로 넘겨주는 함수
     * @param position(Int): 현재 리스트의 인덱스
     * @author Tae hyun Park | Seunggun Sin
     * @since 2022-08-18 | 2022-08-25
     */
    private fun moveToPostDetailPage(position: Int) {
        Log.d(TAG, "클릭한 게시물 id : ${feedList[position]!!.postId}")
        val intent = Intent(context, PostDetailActivity::class.java)
        intent.putExtra("postId", feedList[position]!!.postId)
        startActivity(intent)
    }
}