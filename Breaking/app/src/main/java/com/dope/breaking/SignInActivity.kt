package com.dope.breaking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dope.breaking.databinding.ActivitySignInBinding

class SignInActivity: AppCompatActivity() {

    // 전역 변수로 바인딩 객체 선언
    private var mbinding: ActivitySignInBinding? = null

    // 매번 null 체크할 필요 없이 바인딩 변수 재 선언
    private val binding get() = mbinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 액티비티에서 사용할 바인딩 클래스의 인스턴스 생성
        mbinding = ActivitySignInBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }

}