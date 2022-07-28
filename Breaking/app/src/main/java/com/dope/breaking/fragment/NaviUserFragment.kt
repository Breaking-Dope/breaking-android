package com.dope.breaking.fragment

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dope.breaking.*
import com.dope.breaking.adapter.UserViewPagerAdapter
import com.dope.breaking.databinding.FragmentNaviUserBinding
import com.dope.breaking.follow.Follow
import com.dope.breaking.model.response.DetailUser
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.User
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import com.google.android.material.tabs.TabLayoutMediator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NaviUserFragment : Fragment() {
    private lateinit var binding: FragmentNaviUserBinding // 바인딩 객체
    private var init = true // onCreateView 호출되는 시점을 구분하기 위해 사용

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val user = arguments?.getSerializable("user") as User // 로딩 Fragment 로부터 받아온 유저 객체
        binding = FragmentNaviUserBinding.inflate(inflater, container, false)
        initProfileView(user) // 초기 화면 세팅

        // 프로필 이미지 클릭 시
        binding.imgViewMyPageProfile.setOnClickListener {
            moveToExpandedProfile(user.profileImgURL)
        }
        // 프로필 편집 버튼 클릭 시
        binding.btnEditProfile.setOnClickListener {
            getUserDetailInfo()
        }

        binding.tvFollowValue.setOnClickListener {
            Follow.moveToFollowInfo(
                requireActivity(),
                true,
                ResponseExistLogin.baseUserInfo?.userId!!
            )
        }
        binding.tvFollowTitle.setOnClickListener {
            Follow.moveToFollowInfo(
                requireActivity(),
                true,
                ResponseExistLogin.baseUserInfo?.userId!!
            )
        }

        binding.tvFollowerValue.setOnClickListener {
            Follow.moveToFollowInfo(
                requireActivity(),
                false,
                ResponseExistLogin.baseUserInfo?.userId!!
            )
        }
        binding.tvFollowerTitle.setOnClickListener {
            Follow.moveToFollowInfo(
                requireActivity(),
                false,
                ResponseExistLogin.baseUserInfo?.userId!!
            )
        }
        return binding.root
    }

    /**
     * 이 Fragment 호출 시 초기 화면에 보여줄 View 세팅 함수
     * @param userData(User): 로딩 Fragment 로 부터 받아온 유저 프로필 객체
     * @author Seunggun Sin
     * @since 2022-07-21 | 2022-07-22
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

        val viewPager = binding.viewPager // ViewPager2 객체
        val tabLayout = binding.tabLayout // TabLayout 객체
        viewPager.adapter =
            UserViewPagerAdapter(parentFragmentManager, lifecycle) // ViewPager 어댑터 지정

        // TabLayout 과 ViewPager 를 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // 각 탭의 아이콘 지정
            tab.icon =
                AppCompatResources.getDrawable(requireActivity(), ValueUtil.TAB_ICONS[position])
        }.attach()

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
            .load(ValueUtil.IMAGE_BASE_URL + userData.profileImgURL) // url 에 대한 이미지 호출
            .circleCrop() // 이미지 원형으로 만들기
            .diskCacheStrategy(DiskCacheStrategy.ALL) // 이미지 캐싱
            .error(R.drawable.ic_default_profile_image) // url 호출 에러 시 기본 이미지
            .into(binding.imgViewMyPageProfile)
    }

    /**
     * 프로필 편집 버튼 클릭 시 프로필 편집 페이지로 이동하는 함수
     * @param detailUserData(DetailUser): 프로필 편집을 위한 기존 유저의 데이터 클래스
     */
    private fun moveToEditProfile(detailUserData: DetailUser) {
        init = false // 다시 이 페이지로 돌아와서 onStart 생명주기를 사용하기 위해 flag 값 전환
        val intent = Intent(requireActivity(), EditProfileActivity::class.java)
        intent.putExtra("detail", detailUserData)
        startActivity(intent)
    }

    /**
     * 프로필 이미지 클릭 시, 이미지 확대 페이지로 이동 (Transition Animation 적용)
     * @param url(String): 현재 프로필 페이지에 보여지고 있는 이미지에 대한 url
     * @author Seunggun Sin
     * @since 2022-07-25
     */
    private fun moveToExpandedProfile(url: String) {
        val intent = Intent(requireActivity(), ImageExpansionActivity::class.java)
        intent.putExtra("imgUrl", url)
        val opt = ActivityOptions.makeSceneTransitionAnimation( // Transition Animation
            requireActivity(),
            binding.imgViewMyPageProfile,
            "imgTrans" // 지정된 transition 이름에 해당하는 View 를 기준
        )
        startActivity(intent, opt.toBundle())
    }


    /**
     * 현재 로그인한 유저의 Jwt 토큰을 이용하여 프로필 편집에 필요한 기존 유저 데이터를 요청하는 함수
     * 동시에 요청 성공 시 프로필 편집 페이지로 이동한다.
     * @author Seunggun Sin
     * @since 2022-07-25
     */
    private fun getUserDetailInfo() {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val token = JwtTokenUtil(requireActivity()).getTokenFromLocal() // 로컬에 저장된 토큰 가져오기
        if (token.isNotEmpty())
        // 기존 유저 데이터 요청
            service.requestDetailUserInfo(ValueUtil.JWT_REQUEST_PREFIX + token)
                .enqueue(object : Callback<DetailUser?> {
                    override fun onResponse(
                        call: Call<DetailUser?>,
                        response: Response<DetailUser?>
                    ) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null) // 성공하고 정상적인 값이 존재하면
                                moveToEditProfile(body) // 프로필 편집 페이지로 이동
                        } else {
                            DialogUtil().SingleDialog(
                                requireContext(),
                                "정보를 불러오지 못했습니다. 재시도 바랍니다. ",
                                "확인"
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<DetailUser?>, t: Throwable) {
                        DialogUtil().SingleDialog(
                            requireContext(),
                            "서버에 문제가 발생하였습니다. 재시도 바랍니다.",
                            "확인"
                        ).show()
                    }
                })
        else
            DialogUtil().SingleDialog(requireContext(), "사용자를 식별할 수 없습니다! 앱을 재실행바랍니다.", "확인").show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        activity?.menuInflater?.inflate(R.menu.title_menu, menu)
    }

    /**
     * onStart Lifecycle - 이 Fragment 로부터 벗어나고 다시 돌아왔을 때 프로필 페이지 갱신을 위함
     */
    override fun onStart() {
        super.onStart()
        if (!init) { // onCreateView() 가 이미 호출된 적이 있을 때 & 다시 이 Fragment 로 돌아왔을 때
            // 유저 Fragment 를 리프레시 하기 위해 LoadingFragment 로 전환
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fl_board, LoadingFragment())
                .commit()
        }
    }
}