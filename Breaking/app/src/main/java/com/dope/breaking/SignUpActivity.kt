package com.dope.breaking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.widget.Toast
import com.dope.breaking.databinding.ActivitySignInBinding
import com.dope.breaking.databinding.ActivitySignUpBinding
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity() {

    // 전역 변수로 바인딩 객체 선언
    private var mbinding: ActivitySignUpBinding? = null

    // 매번 null 체크할 필요 없이 바인딩 변수 재 선언
    private val binding get() = mbinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 닉네임 정규표현식 설정
        binding.etNickname.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            val ps: Pattern =
                Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\\\u318D\\\\u119E\\\\u11A2\\\\u2022\\\\u2025a\\\\u00B7\\\\uFE55]+\$") // 한글, 숫자, 영문만 가능하도록 설정
            if (source == "" || ps.matcher(source).matches()) {
                return@InputFilter source
            }
            Toast.makeText( this, "한글, 영문, 숫자만 입력 가능합니다.", Toast.LENGTH_SHORT).show()
            ""
        }, InputFilter.LengthFilter(10))

    }
}