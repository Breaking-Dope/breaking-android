package com.dope.breaking

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.util.JwtTokenUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity.kt"


    companion object {
        private const val DURATION: Long = 1500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 애니메이션 적용
        Handler().postDelayed({
            CoroutineScope(Dispatchers.Main).launch {
                val jwtTokenUtil = JwtTokenUtil(applicationContext)

                // 로컬에 Jwt 토큰이 저장되어 있다면
                if (jwtTokenUtil.getTokenFromLocal() != null) {
                    Log.d(TAG, "TEST1 : " + jwtTokenUtil.getTokenFromLocal())
                    // Jwt 토큰을 이용해 기본 유저 정보 요청
                    try {
                        val userData =
                            jwtTokenUtil.validateJwtToken("Bearer " + jwtTokenUtil.getTokenFromLocal()!!)

                        // 메인 페이지로 유저 데이터와 함께 이동
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.putExtra("userInfo", userData)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    } catch (e: ResponseErrorException) { // 토큰은 있는데 계정 정보가 없을 때 처리
                        val intent = Intent(applicationContext, SignInActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        finish()
                    }
                } else { // 로컬에 토큰이 저장되어 있지않다면
                    // 로그인 페이지로 이동
                    Log.d(TAG, "TEST2")
                    val intent = Intent(applicationContext, SignInActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                }
            }
        }, DURATION)
    }
}