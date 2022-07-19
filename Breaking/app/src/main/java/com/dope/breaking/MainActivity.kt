package com.dope.breaking

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.dope.breaking.databinding.ActivityMainBinding
import com.dope.breaking.fragment.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity.kt"  // Log Tag

    private var mbinding: ActivityMainBinding? = null  // 전역 변수로 바인딩 객체 선언

    private val binding get() = mbinding!!     // 매번 null 체크할 필요 없이 바인딩 변수 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar) // 툴 바 설정
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) // 왼쪽 상단 버튼 만들기
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_navi_menu_bar) // 왼쪽 상단 아이콘
        supportActionBar!!.setDisplayShowTitleEnabled(true) // 툴 바에 타이틀 보이게

        binding.bnvMain.selectedItemId = R.id.menu_breaking_home // 기본 네비게이션 화면을 메인 화면으로 설정..
        if (binding.bnvMain.selectedItemId == R.id.menu_breaking_home) // 기본 화면이 메인이라면 프레그먼트 띄워주기
            changeFragment(NaviHomeFragment())
        bottomNavigationClicked() // 바텀 네비게이션 뷰 내 아이템 클릭 이벤트
        NavigationDrawerClicked() // 네비게이션 드로어 내 아이템 클릭 이벤트 메소드
    }

    /**
    @description - 네비게이션 드로어에서 아이템을 클릭했을 때 발생되는 리스너
    @param - item(MenuItem) : 네비게이션 드로어의 아이템 객체
    @return - Boolean
    @author - Tae hyun Park
    @since - 2022-07-19
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 클릭된 메뉴 아이템의 아이디마다 when 구절로 클릭시 동작을 설정
        when(item!!.itemId){
            android.R.id.home ->{ // 툴 바 메뉴가 클릭된다면
                binding.layoutDrawer.openDrawer(GravityCompat.START)    // 네비게이션 드로어 열기
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
    @description - 사용자가 뒤로가기 버튼을 눌렀을 때 발생되는 리스너
    @param - None
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-19
     */
    override fun onBackPressed() {
        if(binding.layoutDrawer.isDrawerOpen(GravityCompat.START)){ // 드로어가 열려있다면 닫는다.
            binding.layoutDrawer.closeDrawers()
        }else{
            super.onBackPressed()
        }
    }

    /**
    @description - 해당 프레그먼트를 FrameLayout 에서 보여주기 위한 메소드
    @param - fragment(Fragment) : 프레그먼트 객체
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-19
     */
    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fl_board, fragment)
            .commit()
    }

    /**
    @description - 바텀 네비게이션 뷰 내 아이템 클릭 이벤트 메소드
    @param - None
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-19
     */
    private fun bottomNavigationClicked(){
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
                    R.id.menu_breaking_chat -> { // 브레이킹 채팅 버튼을 누르면
                        NaviChatFragment()
                    }
                    else -> { // 그 외는 유저 버튼을 누른 것으로 간주
                        NaviUserFragment()
                    }
                }
            )
            true
        }
    }

    /**
    @description - 네비게이션 드로어 내 아이템 클릭 이벤트 메소드
    @param - None
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-19
     */
    private fun NavigationDrawerClicked(){
        binding.viewNavigationDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_drawer_point -> { // 브레이킹 포인트 버튼을 누르면
                    val intent = Intent(this, NaviPointActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this,"Pressed Point Screen",Toast.LENGTH_SHORT).show()
                }
                R.id.menu_drawer_bookmark -> { // 브레이킹 북마크 버튼을 누르면
                    val intent = Intent(this, NaviBookActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this,"Pressed Bookmark Screen",Toast.LENGTH_SHORT).show()
                }
                R.id.menu_drawer_cart -> { // 브레이킹 구매한 제보 버튼을 누르면
                    val intent = Intent(this, NaviPurchaseActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this,"Pressed Cart Screen",Toast.LENGTH_SHORT).show()
                }
                R.id.menu_drawer_pencil -> { // 브레이킹 프로필 편집 버튼을 누르면
                    val intent = Intent(this, NaviProfileActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this,"Pressed Profile Screen",Toast.LENGTH_SHORT).show()
                }
                else -> { // 그 외는 설정 버튼을 누른 것으로 간주
                    val intent = Intent(this, NaviSettingActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this,"Pressed Setting Screen",Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }
}