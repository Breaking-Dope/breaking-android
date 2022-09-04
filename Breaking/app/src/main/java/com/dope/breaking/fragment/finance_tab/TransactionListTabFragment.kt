package com.dope.breaking.fragment.finance_tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.adapter.TransactionAdapter
import com.dope.breaking.databinding.FragmentFinanceTransactionBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.finance.FinanceManager
import com.dope.breaking.model.response.ResponseTransaction
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionListTabFragment : Fragment() {
    private lateinit var binding: FragmentFinanceTransactionBinding
    private val transactionList = mutableListOf<ResponseTransaction?>() // 거래 내역 리스트
    private lateinit var adapter: TransactionAdapter // 거래 어댑터
    private var isObtainedAll = false // 아이템을 다 받아왔는지 판단
    private var isLoading = false // 로딩 중 판단
    private val financeManager = FinanceManager() // 거래 매니저

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFinanceTransactionBinding.inflate(inflater, container, false)

        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(requireContext()).getAccessTokenFromLocal()

        /*
            최초 입출금 내역 요청
         */
        processTransactionList(0, token, {
            isLoading = true
        }, { it ->
            transactionList.addAll(it) // 받아온 아이템 리스트 리스트에 추가
            adapter = TransactionAdapter(requireContext(), transactionList)

            // 리스트가 비어있다면
            if (it.isEmpty()) {
                // 비어있는 텍스트 처리
                binding.tvFinanceEmpty.visibility = View.VISIBLE
                binding.rcvFinance.visibility = View.GONE
            } else {
                binding.tvFinanceEmpty.visibility = View.GONE
                binding.rcvFinance.visibility = View.VISIBLE
            }
            // 어댑터 적용
            binding.rcvFinance.adapter = adapter

            // 로딩 종료
            isLoading = false

            // 스크롤 이벤트 리스너
            binding.rcvFinance.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    // 마지막 아이템 인덱스
                    val lastIndex =
                        (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                    // 가져온 아이템 사이즈가 가져와야하는 사이즈보다 작은 경우 새로운 요청을 못하게 막기
                    if (recyclerView.adapter!!.itemCount < ValueUtil.TRANSACTION_SIZE) {
                        return
                    }

                    if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainedAll && !isLoading) {
                        processTransactionList(transactionList[lastIndex]!!.cursorId, token, {
                            adapter.addItem(null) // 로딩 아이템 추가
                            isLoading = true // 로딩 시작
                        }, {
                            // 가져와야하는 리스트보다 적은 경우
                            if (it.size < ValueUtil.TRANSACTION_SIZE) {
                                adapter.removeLast() // 로딩 아이템 제거
                                if (it.isNotEmpty())
                                    adapter.addItems(it) // 아이템 추가
                                isObtainedAll = true // 모두 받아왔다는 flag 설정
                            } else {
                                adapter.removeLast() // 로딩 아이템 제거
                                adapter.addItems(it) // 리스트 추가
                            }
                            isLoading = false // 로딩 종료
                        }, {
                            it.printStackTrace()
                        })
                    }
                }
            })
        }, {
            it.printStackTrace()
        })

        return binding.root
    }

    /**
     * 입출금 내역을 요청하는 프로세스 함수
     * @param cursorId(Int): 마지막으로 요청한 리스트의 마지막 인덱스
     * @param token(String): 본인의 Jwt 토큰
     * @param init(() -> Unit): 요청 전 초기 함수
     * @param last((List<ResponseTransaction>) -> Unit): 요청 후처리 함수
     * @param error((ResponseErrorException) -> Unit): 에러 함수
     * @author Seunggun Sin
     * @since 2022-09-03
     */
    private fun processTransactionList(
        cursorId: Int,
        token: String,
        init: () -> Unit,
        last: (List<ResponseTransaction>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init()
            try {
                val responseList =
                    financeManager.startGetFinanceList(token, cursorId, ValueUtil.TRANSACTION_SIZE)

                last(responseList)
            } catch (e: ResponseErrorException) {
                error(e)
            }
        }
    }
}