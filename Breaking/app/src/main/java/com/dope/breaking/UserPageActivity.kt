package com.dope.breaking

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dope.breaking.adapter.UserViewPagerAdapter
import com.dope.breaking.exception.MissingJwtTokenException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.follow.Follow
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.User
import com.dope.breaking.user.UserProfile
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.ValueUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getLongExtra("userId", 0) // 받아온 프로필 보여줄 유저의 고유 id
        val userProfile = UserProfile(this)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = userProfile.getUserProfileInfo(userId) // 해당 유저 정보 가져오기
                setContentView(R.layout.fragment_navi_user)

                initView(userId, user) // 유저 레이아웃 초기 설정
                setFollowButton(user.isFollowing) // 팔로우 버튼 설정
                setPostLayout() // 게시글 레이아웃 설정
                fillDataInView(user) // view 에 유저 데이터 뿌려주기
                setButtonEvents(user) // 버튼 이벤트 설정
            } catch (e: ResponseErrorException) {
                DialogUtil().SingleDialog(
                    this@UserPageActivity,
                    "정보를 불러오지 못했습니다. 재시도 바랍니다. ",
                    "확인"
                ) {
                    finish()
                }.show()
            } catch (e: MissingJwtTokenException) {
                DialogUtil().SingleDialog(
                    this@UserPageActivity,
                    "사용자를 식별할 수 없습니다! 앱을 재실행바랍니다.",
                    "확인"
                ) {
                    finish()
                }.show()
            } catch (e: Exception) {
                DialogUtil().SingleDialog(this@UserPageActivity, "예기치 못한 문제가 발생하였습니다.", "확인") {
                    finish()
                }.show()
            }
        }
    }

    /**
     * 유저 구분을 통해 초기 레이아웃 설정하는 함수(유저 Fragment 재활용)
     * - 본인: 툴바 뒤로 가기 버튼 추가, 타이틀 닉네임 변경, 팔로우 버튼 삭제
     * - 다른 유저: 툴바 뒤로 가기 버튼 추가, 타이틀 닉네임 변경, 프로필 편집 버튼 제거, 팔로우 버튼 활성화, 설정 버튼 제거
     * @param userId(Long): 프로필 페이지 대상 고유 id
     * @param user(User): 응답으로 받은 대상 유저 정보 객체
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    private fun initView(userId: Long, user: User) {
        val toolbar = findViewById<Toolbar>(R.id.my_page_tool_bar)

        if (userId == ResponseExistLogin.baseUserInfo?.userId) { // 본인인 경우
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.setting_action -> { // 툴바의 톱니바퀴 아이콘 클릭 시, 환경설정 액티비티로 이동
                        startActivity(Intent(this, NaviSettingActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            val button = findViewById<Button>(R.id.btn_edit_profile)
            button.setOnClickListener {
                UserProfile(this@UserPageActivity).getUserDetailInfo()
            }
            val followButton = findViewById<Button>(R.id.btn_follow)
            followButton.visibility = View.GONE
            toolbar.title = user.nickname // 타이틀 닉네임으로

            // 뒤로가기 버튼 설정
            toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_black_24)
            toolbar.setNavigationOnClickListener {
                finish()
            }
        } else { // 다른 유저인 경우
            toolbar.title = user.nickname // 타이틀 닉네임으로
            toolbar.menu.clear() // 메뉴 아이콘 제거
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 활성화
            findViewById<Button>(R.id.btn_edit_profile).visibility = View.GONE
        }
    }

    /**
     * 로그인한 유저가 대상 프로필 페이지를 보는 관점에서 유저가 이 대상을 팔로우했는지 판단
     * 그에 따른 버튼 및 텍스트 설정
     * @param isFollowing(Boolean): 팔로우했는지 안했는지 여부
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    private fun setFollowButton(isFollowing: Boolean) {
        val followButton = findViewById<Button>(R.id.btn_follow)
        if (isFollowing) {
            followButton.text = "팔로잉"
            followButton.setBackgroundResource(R.drawable.feed_category_button_background)
        } else {
            followButton.text = "팔로우"
            followButton.setBackgroundResource(R.drawable.sign_up_user_type_selected)
        }
    }

    /**
     * 응답으로 받아온 대상 유저의 데이터를 바탕으로 유저 프로필 view 에 뿌려주기
     * @param user(User): 대상 유저 데이터 객체
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    private fun fillDataInView(user: User) {
        findViewById<TextView>(R.id.tv_my_page_nickname).text = user.nickname
        findViewById<TextView>(R.id.tv_my_page_status).text = user.statusMsg
        findViewById<TextView>(R.id.tv_follow_value).text = user.followingCount.toString()
        findViewById<TextView>(R.id.tv_follower_value).text = user.followerCount.toString()
        findViewById<TextView>(R.id.tv_post_value).text = user.postCount.toString()

        Glide.with(this@UserPageActivity) // 이미지 보여주기
            .load(ValueUtil.IMAGE_BASE_URL + user.profileImgURL) // url 에 대한 이미지 호출
            .circleCrop() // 이미지 원형으로 만들기
            .diskCacheStrategy(DiskCacheStrategy.ALL) // 이미지 캐싱
            .error(R.drawable.ic_default_profile_image) // url 호출 에러 시 기본 이미지
            .into(findViewById(R.id.img_view_my_page_profile))
    }

    /**
     * 유저 페이지에 있는 각 view 에 대한 클릭 이벤트 지정
     * @param user(User): 대상 유저 데이터 객체
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    private fun setButtonEvents(user: User) {
        // 팔로우 & 팔로워 타이틀, 값 클릭 시 리스트 페이지로 이동
        findViewById<TextView>(R.id.tv_follow_value).setOnClickListener {
            Follow.moveToFollowInfo(
                this@UserPageActivity,
                true,
                user.userId
            )
        }
        findViewById<TextView>(R.id.tv_follow_title).setOnClickListener {
            Follow.moveToFollowInfo(
                this@UserPageActivity,
                true,
                user.userId
            )
        }

        findViewById<TextView>(R.id.tv_follower_value).setOnClickListener {
            Follow.moveToFollowInfo(
                this@UserPageActivity,
                false,
                user.userId
            )
        }
        findViewById<TextView>(R.id.tv_follower_title).setOnClickListener {
            Follow.moveToFollowInfo(
                this@UserPageActivity,
                false,
                user.userId
            )
        }

        // 프로필 이미지 클릭 시
        val profile = findViewById<ImageView>(R.id.img_view_my_page_profile)
        profile.setOnClickListener {
            if (user.profileImgURL != null) {
                UserProfile(this@UserPageActivity).moveToExpandedProfile(
                    user.profileImgURL,
                    profile
                )
            }
        }
    }

    /**
     * 게시글 레이아웃 부분 설정
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    private fun setPostLayout() {
        val viewPager = findViewById<ViewPager2>(R.id.view_pager) // ViewPager2 객체
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout) // TabLayout 객체
        viewPager.adapter =
            UserViewPagerAdapter(supportFragmentManager, lifecycle) // ViewPager 어댑터 지정

        // TabLayout 과 ViewPager 를 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // 각 탭의 아이콘 지정
            tab.icon =
                AppCompatResources.getDrawable(this@UserPageActivity, ValueUtil.TAB_ICONS[position])
        }.attach()
    }

    /**
     * 툴바 아이템 클릭 이벤트
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}