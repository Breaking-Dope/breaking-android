package com.dope.breaking

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.dope.breaking.databinding.ActivityMainBinding
import com.dope.breaking.fragment.*
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.util.DialogUtil

class MainActivity : AppCompatActivity() {

    private var mbinding: ActivityMainBinding? = null  // 전역 변수로 바인딩 객체 선언

    private val binding get() = mbinding!!     // 매번 null 체크할 필요 없이 바인딩 변수 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mbinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        if (intent != null) {
            try {
                ResponseExistLogin.baseUserInfo =
                    intent.getSerializableExtra("userInfo") as ResponseExistLogin
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }

        binding.bnvMain.selectedItemId = R.id.menu_breaking_home // 기본 네비게이션 화면을 메인 화면으로 설정..

        if (binding.bnvMain.selectedItemId == R.id.menu_breaking_home) // 기본 화면이 메인이라면 프레그먼트 띄워주기
            changeFragment(NaviHomeFragment())

        bottomNavigationClicked() // 바텀 네비게이션 뷰 내 아이템 클릭 이벤트
    }

    /**
     * 해당 프레그먼트를 FrameLayout 에서 보여주기 위한 메소드 (현재 Fragment 재 클릭 시 무시)
     * @param fragment(Fragment) : 프레그먼트 객체
     * @author - Tae hyun Park | Seunggun Sin
     * @since 2022-07-19 | 2022-09-14
     */
    private fun changeFragment(fragment: Fragment) {
        val current = supportFragmentManager.findFragmentById(R.id.fl_board) // 현재 보여지는 Fragment 객체
        if (current is NaviHomeFragment && fragment is NaviHomeFragment)
            return
        if (current is NaviUserFragment && fragment is LoadingFragment)
            return
        if (current is NaviChartFragment && fragment is NaviChartFragment)
            return
        if (current is NaviMissionFragment && fragment is NaviMissionFragment)
            return
        if (current is NaviChatFragment && fragment is NaviChatFragment)
            return

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fl_board, fragment)
            .commit()
    }

    /**
    * 바텀 네비게이션 뷰 내 아이템 클릭 이벤트 메소드
    * @author Tae hyun Park | Seunggun Sin
    * @since 2022-07-19 | 2022-09-14
     */
    private fun bottomNavigationClicked() {
        binding.bnvMain.setOnItemSelectedListener { item ->
            changeFragment(
                when (item.itemId) {
                    R.id.menu_breaking_chart -> { // 브레이킹 차트 버튼을 누르면
                        NaviChartFragment()
                    }
                    R.id.menu_breaking_mission -> { // 브레이킹 미션 버튼을 누르면
                        NaviMissionFragment()
                    }
                    R.id.menu_breaking_home -> { // 브레이킹 메인 화면 버튼을 누르면
                        NaviHomeFragment()
                    }
                    else -> { // 그 외는 유저 버튼을 누른 것으로 간주
                        // 로그인이 안되어있을 경우
                        if (ResponseExistLogin.baseUserInfo == null) {
                            // 로그인 페이지 이동 유도
                            DialogUtil().MultipleDialog(
                                this,
                                "로그인이 필요합니다. 로그인 하러 가시겠습니까?",
                                "취소",
                                "이동",
                                {},
                                {
                                    startActivity(Intent(this, SignInActivity::class.java))
                                }).show()
                            NaviHomeFragment()
                        } else
                            LoadingFragment()
                    }
                }
            )
            true
        }
    }
}