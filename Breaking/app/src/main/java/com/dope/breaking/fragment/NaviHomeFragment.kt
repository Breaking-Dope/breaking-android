package com.dope.breaking.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.dope.breaking.R
import com.dope.breaking.adapter.UserPostAdapter
import com.dope.breaking.board.PostActivity
import com.dope.breaking.databinding.FragmentNaviHomeBinding
import com.dope.breaking.model.response.ResponseMainFeed
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NaviHomeFragment : Fragment() {
    private var mbinding: FragmentNaviHomeBinding? = null // 바인딩 변수 초기화
    private val binding get() = mbinding!! // 바인딩 변수 재할당

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mbinding = FragmentNaviHomeBinding.inflate(inflater, container, false)

        // 피드 가져오는 요청 임시 처리 - 리팩토링 할 예정
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        service.requestGetMainFeed(
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(requireActivity()).getTokenFromLocal(),
            0,
            16 // 가져올 게시글 개수
        ).enqueue(object : Callback<List<ResponseMainFeed>?> {
            override fun onResponse(
                call: Call<List<ResponseMainFeed>?>,
                response: Response<List<ResponseMainFeed>?>
            ) {
                if (response.isSuccessful) {
                    val list = mutableListOf<ResponseMainFeed>()
                    val resList = response.body()

                    resList?.forEach {
                        list.add(it)
                    }

                    val adapter = UserPostAdapter(requireActivity(), list)
                    binding.rcvMainFeed.addItemDecoration(
                        DividerItemDecoration(
                            requireActivity(),
                            LinearLayout.VERTICAL
                        )
                    )
                    binding.rcvMainFeed.adapter = adapter
                }
            }

            override fun onFailure(call: Call<List<ResponseMainFeed>?>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
        // 제보하기 버튼을 누르면 게시글 작성 페이지로 이동
        binding.fabPosting.setOnClickListener {
            val intent = Intent(activity, PostActivity::class.java)
            startActivity(intent)
        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        activity?.menuInflater?.inflate(R.menu.general_title_menu, menu) // 툴 바 아이콘 변경
    }
}