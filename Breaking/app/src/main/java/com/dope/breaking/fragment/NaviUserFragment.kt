package com.dope.breaking.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dope.breaking.NaviSettingActivity
import com.dope.breaking.R
import com.dope.breaking.databinding.FragmentNaviUserBinding
import com.dope.breaking.model.response.User
import com.dope.breaking.model.response.ResponseExistLogin

class NaviUserFragment : Fragment() {
    private lateinit var binding: FragmentNaviUserBinding // 바인딩 객체

    companion object {
        private const val IMAGE_BASE_URL = "https://team-dope.link:8443" // 이미지 호출을 위한 서버 base url
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        val user = arguments?.getSerializable("user") as User // 로딩 Fragment 로부터 받아온 유저 객체
        binding = FragmentNaviUserBinding.inflate(inflater, container, false)
        initProfileView(user) // 초기 화면 세팅

        return binding.root
    }

    /**
     * 이 Fragment 호출 시 초기 화면에 보여줄 View 세팅 함수
     * @param userData(User): 로딩 Fragment 로 부터 받아온 유저 프로필 객체
     * @author Seunggun Sin
     * @since 2022-07-21
     */
    private fun initProfileView(userData: User) {
        /*
            받아온 유저 데이터를 view 에 뿌려주기
         */
        binding.tvMyPageNickname.text = userData.nickname
        binding.tvMyPageStatus.text = userData.statusMsg
        binding.tvFollowValue.text = userData.followingCount.toString()
        binding.tvFollowerValue.text = userData.followerCount.toString()
        binding.tvPostValue.text = userData.postCount.toString()

        binding.myPageToolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.setting_action -> { // 툴바의 톱니바퀴 아이콘 클릭 시, 환경설정 액티비티로 이동
                    startActivity(Intent(requireActivity(), NaviSettingActivity::class.java))
                    true
                }
                else -> false
            }
        }

        Glide.with(this) // 이미지 보여주기
            .load(IMAGE_BASE_URL + ResponseExistLogin.baseUserInfo!!.profileImgUrl) // url에 대한 이미지 호출
            .circleCrop() // 이미지 원형으로 만들기
            .diskCacheStrategy(DiskCacheStrategy.ALL) // 이미지 캐싱
            .into(binding.imgvMyPageProfile)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        activity?.menuInflater?.inflate(R.menu.title_menu, menu)
    }
}