package com.dope.breaking.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.dope.breaking.R
import com.dope.breaking.board.PostActivity
import com.dope.breaking.databinding.FragmentNaviHomeBinding

class NaviHomeFragment : Fragment() {
    private var mbinding: FragmentNaviHomeBinding? = null // 바인딩 변수 초기화
    private val binding get() = mbinding!! // 바인딩 변수 재할당

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mbinding = FragmentNaviHomeBinding.inflate(inflater, container, false)
        // 제보하기 버튼을 누르면 게시글 작성 페이지로 이동
        binding.fabPosting.setOnClickListener {
            var intent = Intent(activity, PostActivity::class.java)
            startActivity(intent)
        }
        return binding.root
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        activity?.menuInflater?.inflate(R.menu.general_title_menu, menu) // 툴 바 아이콘 변경
    }
}