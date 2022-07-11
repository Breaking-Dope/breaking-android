package com.dope.breaking

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.dope.breaking.databinding.ActivitySignUpBinding
import com.dope.breaking.exception.MissingJwtTokenException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.RequestSignUp
import com.dope.breaking.model.ResponseLogin
import com.dope.breaking.model.ResponseJwtUserInfo
import com.dope.breaking.signup.Register
import com.dope.breaking.util.JwtTokenUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity() {

    // 전역 변수로 바인딩 객체 선언
    private var mbinding: ActivitySignUpBinding? = null

    // 매번 null 체크할 필요 없이 바인딩 변수 재 선언
    private val binding get() = mbinding!!
    private lateinit var responseBody: ResponseLogin // 로그인 시 생성되는 응답 객체
    private lateinit var defaultProfile: Bitmap // bitmap 형태의 기본 프로필 이미지

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // drawable 에 저장된 기본 xml 을 기본 프로필 이미지 bitmap 으로 생성
        defaultProfile =
            AppCompatResources.getDrawable(this, R.drawable.ic_default_profile_image)?.toBitmap()!!

        // 로그인 페이지로부터 받아온 response 객체 할당
        val intentFromSignIn = intent
        if (intentFromSignIn != null) {
            val data = intentFromSignIn.getSerializableExtra("responseBody")
            if (data is ResponseLogin)
                responseBody = data
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 테스트 input 객체 - 기능 구현 완료 시 삭제 바람
            val testInputData = RequestSignUp(
                responseBody.userName,
                "test_nickname",
                "01012345678",
                responseBody.userEmail,
                "홍길동",
                "테스트 회원가입",
                true
            )
            // 회원가입 요청 시작, 회원가입 버튼 클릭하고 검증 완료 후 input 데이터와 함께 호출
            processSignUp(
                inputData = testInputData,
                imageData = defaultProfile,
                imageName = "default.png"
            )
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

    /**
     * 모듈들을 불러서 회원가입 요청을 하는 프로세스 함수
     * @param inputData(RequestSignUp): 필드 데이터 객체
     * @param imageData(Bitmap): bitmap 형태의 프로필 이미지 데이터
     * @param imageName(String): 이미지 데이터의 파일 이름
     * @return None
     * @author Seunggun Sin
     * @since 2022-07-11
     */
    private suspend fun processSignUp(
        inputData: RequestSignUp,
        imageData: Bitmap,
        imageName: String
    ) {
        try {
            val register = Register() // 커스텀 회원가입 객체 생성
            // 회원가입 요청 함수 signature. 아래 형식대로 회원가입 요청 함수 호출.
            // 현재 input 은 테스트용이므로 실제 input 값으로 대체 바람
            val responseHeaders = register.startRequestSignUp(
                inputData = inputData,
                imageData = imageData,
                imageName = imageName
            )

            val tokenUtil = JwtTokenUtil(applicationContext) // Jwt Util 객체 생성
            val token = tokenUtil.getTokenFromResponse(responseHeaders) // 응답으로부터 Jwt 토큰 값 추출

            if (token != null) { // 토큰이 존재하면
                tokenUtil.setToken(token) // SharedPreferences 를 이용하여 Jwt 토큰을 로컬에 저장

                //회원가입 응답으로 받아온 토큰 값을 토대로 유저의 기본 정보 가져오는 요청
                val userInfo = tokenUtil.validateJwtToken("Bearer $token")
                moveToMainPage(userInfo) // 가져온 유저 정보 값을 가지고 메인 페이지로 이동하기
            } else {
                // 존재하지 않는 Jwt 토큰 케이스에 대한 예외 던지기
                throw MissingJwtTokenException("Jwt 토큰이 존재하지 않습니다.")
            }
        } catch (e: ResponseErrorException) {
            e.printStackTrace()
            // 응답 에러에 대한 예외 처리하기
        } catch (e: MissingJwtTokenException) {
            // 토큰 값이 존재하지 않을 때에 대한 예외 처리하기
        }
    }

    /**
     * 유저 정보를 갖고 메인 피드 페이지로 이동하는 메소드 (임시로 MainActivity 로 설정)
     * @param userInfo(ResponseJwtUserInfo): Jwt 토큰 확인을 통해 받은 Response DTO 객체
     * @return None
     * @author Seunggun Sin
     * @since 2022-07-11
     */
    private fun moveToMainPage(userInfo: ResponseJwtUserInfo) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("userInfo", userInfo)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}