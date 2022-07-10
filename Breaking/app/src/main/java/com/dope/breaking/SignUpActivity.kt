package com.dope.breaking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.widget.Toast
import com.dope.breaking.databinding.ActivitySignUpBinding
import com.dope.breaking.exception.MissingJwtTokenException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.ResponseLogin
import com.dope.breaking.signup.Register
import com.dope.breaking.util.JwtTokenUtil
import okhttp3.Headers
import retrofit2.Response
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity() {

    // 전역 변수로 바인딩 객체 선언
    private var mbinding: ActivitySignUpBinding? = null

    // 매번 null 체크할 필요 없이 바인딩 변수 재 선언
    private val binding get() = mbinding!!
    private lateinit var responseBody: ResponseLogin // 로그인 시 생성되는 응답 객체

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 페이지로부터 받아온 response 객체 할당
        val intentFromSignIn = intent
        responseBody = intentFromSignIn.getSerializableExtra("responseBody") as ResponseLogin

        val register = Register() // 커스텀 회원가입 객체 생성

        try {
            // 회원가입 요청 함수 signature. 아래 형식대로 회원가입 요청 함수 호출.
//        val responseHeaders = register.startRequestSignUp(inputData =, imageData =, imageName =)

            val responseHeaders: Headers? = null // 테스트용 코드 - 요청 함수에 값 전달하는 기능 완성 시 삭제 바람.
            val responseHeaders2 = responseHeaders!! // 테스트용 코드 - 요청 함수에 값 전달하는 기능 완성 시 삭제 바람.
            // 상단 테스트용 코드 두줄을 지우지 않으면 하단 코드들은 NPE 발생

            val tokenUtil = JwtTokenUtil(this) // Jwt Util 객체 생성
            val token = tokenUtil.getTokenFromResponse(responseHeaders) // 응답으로부터 Jwt 토큰 값 추출
            if (token != null) {
                tokenUtil.setToken(token) // SharedPreferences 를 이용하여 Jwt 토큰을 로컬에 저장
                /*
                    Jwt 토큰을 이용하여 유저 정보 요청하는 부분 필요
                */
            } else {
                // 존재하지 않는 Jwt 토큰 케이스에 대한 예외 던지기
                throw MissingJwtTokenException("Jwt 토큰이 존재하지 않습니다.")
            }
        } catch (e: ResponseErrorException) {
            // 예외 처리하기
        } catch (e: MissingJwtTokenException) {
            // 예외 처리하기
        }

        // 닉네임 정규표현식 설정
        binding.etNickname.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            val ps: Pattern =
                Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\\\u318D\\\\u119E\\\\u11A2\\\\u2022\\\\u2025a\\\\u00B7\\\\uFE55]+\$") // 한글, 숫자, 영문만 가능하도록 설정
            if (source == "" || ps.matcher(source).matches()) {
                return@InputFilter source
            }
            Toast.makeText(this, "한글, 영문, 숫자만 입력 가능합니다.", Toast.LENGTH_SHORT).show()
            ""
        }, InputFilter.LengthFilter(10))

    }
}