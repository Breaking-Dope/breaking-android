package com.dope.breaking

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val DURATION: Long = 1500

class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity.kt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 애니메이션 적용
        Handler(Looper.myLooper()!!).postDelayed({
            CoroutineScope(Dispatchers.Main).launch {
                val jwtTokenUtil = JwtTokenUtil(applicationContext)

                // 로컬에 Jwt 토큰이 저장되어 있다면
                if (jwtTokenUtil.getAccessTokenFromLocal() != "") {
                    // Jwt 토큰을 이용해 기본 유저 정보 요청
                    try {
                        val userData =
                            jwtTokenUtil.validateJwtToken(ValueUtil.JWT_REQUEST_PREFIX + jwtTokenUtil.getAccessTokenFromLocal())

                        moveToMainPage(userData) // 메인 페이지로 이동
                    } catch (e: ResponseErrorException) { // 토큰은 있는데 계정 정보가 없을 때 처리
                        val errorJson = JSONObject(e.message!!) // 에러 json 받아오기

                        if (errorJson.getString("code") == "BSE002") { // 엑세스 토큰이 만료되었을 경우에 대한 에러 처리
                            val result = jwtTokenUtil.reissueJwtToken(
                                jwtTokenUtil.getAccessTokenFromLocal(),
                                jwtTokenUtil.getRefreshTokenFromLocal()
                            ) // 재발급 요청
                            if (result) { // 재발급 및 새 토큰 저장 성공 시
                                try {
                                    val userData =
                                        jwtTokenUtil.validateJwtToken(ValueUtil.JWT_REQUEST_PREFIX + jwtTokenUtil.getAccessTokenFromLocal())
                                    // 유저 정보 요청
                                    moveToMainPage(userData) // 메인 페이지로 이동
                                } catch (e: ResponseErrorException) {
                                    moveToLoginPage() // 자동로그인 실패시, 로그인 페이지로 이동
                                }
                            } else {
                                moveToLoginPage() // 재발급 실패 시 로그인 페이지로 이동
                            }
                        } else { // 나머지 에러의 경우
                            moveToLoginPage() // 로그인 페이지로 이동
                        }
                    }
                } else { // 로컬에 토큰이 저장되어 있지않다면
                    // 로그인 페이지로 이동
                    moveToLoginPage()
                }
            }
        }, DURATION)
    }

    /**
     * 메인 페이지로 이동하는 함수
     * @param userData(ResponseExistLogin): 기존 유저가 로그인했을 경우 얻는 기본 유저 데이터 DTO
     * @author Seunggun Sin
     * @since 2022-08-17
     */
    private fun moveToMainPage(userData: ResponseExistLogin) {
        // 메인 페이지로 유저 데이터와 함께 이동
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.putExtra("userInfo", userData)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    /**
     * 로그인 페이지로 이동하는 함수
     * @author Seunggun Sin
     * @since 2022-08-17
     */
    private fun moveToLoginPage() {
        val intent = Intent(applicationContext, SignInActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}