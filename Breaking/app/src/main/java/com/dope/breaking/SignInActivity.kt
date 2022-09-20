package com.dope.breaking

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dope.breaking.databinding.ActivitySignInBinding
import com.dope.breaking.exception.InvalidAccessTokenException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.request.RequestKakaoToken
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.ResponseLogin
import com.dope.breaking.oauth.GoogleLogin
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.gson.JsonElement
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignInActivity : AppCompatActivity() {
    private val TAG = "SignInActivity.kt"     // Log Tag

    private var mbinding: ActivitySignInBinding? = null  // 전역 변수로 바인딩 객체 선언

    private val binding get() = mbinding!!     // 매번 null 체크할 필요 없이 바인딩 변수 재 선언

    //구글 로그인 intent 호출 후, 로그인 intent 의 결과를 받기 위해 ActivityResult 객체 선언
    private lateinit var googleLoginActivityResult: ActivityResultLauncher<Intent>

    private lateinit var googleLogin: GoogleLogin // 구글 로그인에 필요한 기능들이 담겨있는 커스텀 클래스 선언

    private var responseBody: Any? = null // 카카오 로그인 토큰 검증 요청에 대한 응답을 처리하기 위한 객체

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 액티비티에서 사용할 바인딩 클래스의 인스턴스 생성
        mbinding = ActivitySignInBinding.inflate(layoutInflater)

        setContentView(binding.root)

        googleLogin = GoogleLogin(this) // 구글 로그인 커스텀 클래스 객체 초기화

        val customProgressDialog = DialogUtil().ProgressDialog(this) // 로딩창 progress 객체 생성

        /*
        ActivityResult 객체 초기화 및 동시에 콜백 정의.
        deprecated 된 onActivityResult 콜백 메소드를 대체함.
        이 콜백의 parameter 는 ActivityResult! 타입으로 이 객체에 resultCode 와 data 가 포함되어 있음.
         */
        googleLoginActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) { // 결과가 성공적이라면
                    try {
                        val task =
                            GoogleSignIn.getSignedInAccountFromIntent(it.data) // 구글 로그인 결과 값 가져오기
                        val account = task.result // 계정정보가 담긴 객체 가져오기

                        customProgressDialog.showDialog() // 로딩 progress 시작

                        CoroutineScope(Dispatchers.Main).launch {
                            /*
                            받아온 계정 데이터를 구글 로그인 요청 메소드의 인자로 넘겨줌으로써 토큰 검증 프로세스 시작
                            메소드의 결과로 회원가입이 필요한지 필요하지 않은지 boolean 값을 리턴(jwt 토큰의 유무)
                            true 면 회원가입 필요, false 면 회원가입 필요 x
                            */
                            try {
                                val isExisting = googleLogin.requestGoogleLogin(account)
                                customProgressDialog.dismissDialog() // 네트워크 작업이 끝난 후 로딩창 종료

                                // Jwt 토큰이 없고, response body 가 정상적인 값이 있다면
                                if (!isExisting) {
                                    if (googleLogin.responseBody is ResponseLogin) { // 응답이 신규 유저에 대한 값일 때
                                        // 회원가입 페이지로 이동
                                        moveToSignUpPage(googleLogin.responseBody as ResponseLogin)
                                    } else {
                                        showToast("정보를 불러오는데 실패하였습니다.")
                                    }
                                } else {
                                    if (googleLogin.responseBody is ResponseExistLogin) { // 응답이 기존 유저에 대한 값일 때
                                        // 로그인 처리 및 메인 페이지로 이동
                                        moveToMainPage(googleLogin.responseBody as ResponseExistLogin)
                                    } else {
                                        showToast("정보를 불러오는데 실패하였습니다.")
                                    }
                                }
                            } catch (e: ResponseErrorException) {
                                e.printStackTrace()
                                if (customProgressDialog.isShowing()) { // 로딩 progress 가 실행 중이라면
                                    customProgressDialog.dismissDialog() // 종료
                                }
                                DialogUtil().SingleDialog(
                                    this@SignInActivity,
                                    "요청에 문제가 발생하였습니다.",
                                    "확인"
                                ).show()
                            } catch (e: InvalidAccessTokenException) { // 엑세스 토큰을 인증할 수 없는 예외 처리
                                e.printStackTrace()
                                if (customProgressDialog.isShowing()) {
                                    customProgressDialog.dismissDialog()
                                }
                                DialogUtil().SingleDialog(
                                    this@SignInActivity,
                                    "구글 인증에 문제가 발생하였습니다.",
                                    "확인"
                                ).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                if (customProgressDialog.isShowing()) {
                                    customProgressDialog.dismissDialog()
                                }
                                DialogUtil().SingleDialog(
                                    this@SignInActivity,
                                    "예기치 못한 문제가 발생하였습니다.",
                                    "확인"
                                ).show()
                            }

                        }
                    } catch (e: ApiException) { // ApiException: 구글 로그인 시 발생하는 에러
                        e.printStackTrace()
                        if (customProgressDialog.isShowing()) {
                            customProgressDialog.dismissDialog()
                        }
                        DialogUtil().SingleDialog(this, "구글 로그인 시도에 실패하였습니다.", "확인").show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (customProgressDialog.isShowing()) {
                            customProgressDialog.dismissDialog()
                        }
                        DialogUtil().SingleDialog(this, "예기치 못한 문제가 발생하였습니다. ", "확인").show()
                    }
                }
            }
        // 구글 로그인 버튼 클릭 이벤트
        binding.btnSignInGoogle.setOnClickListener {
            startGoogleLoginIntent()
        }

        // 카카오 로그인 버튼 클릭 이벤트
        binding.btnSignInKakao.setOnClickListener {
            startKakaoLogin()
        }
    }

    /**
     * 구글 로그인에 대한 intent 를 호출한다. 로그인 intent 에 대한 결과를 받기 위해 activityResult 사용
     * @return Unit
     * @author Seunggun Sin
     * @since 2022-07-07
     */
    private fun startGoogleLoginIntent() {
        val signInIntent: Intent =
            googleLogin.googleSignInClient.signInIntent // 구글 로그인 intent 객체 생성
        googleLoginActivityResult.launch(signInIntent) // 결과 값을 받아오는 intent 실행 (startActivityForResult 와 같음)
    }

    /**
    @description - 카카오톡 어플의 설치 여부와 카카오계정의 유무에 따른 로그인 처리 함수 , 콜백함수에서 로그인에 대한 성공, 실패 결과 로직을 진행한다.
    성공할 경우 OAuthToken 타입의 토큰을, 실패할 경우 Throwable 타입의 error 객체를 리턴한다.
    @param - None
    @return - Unit
    @author - Tae hyun Park
    @since - 2022-07-05 | 2022-07-21
     **/
    private fun startKakaoLogin() {
        // 카카오계정 로그인 공통 callback
        // 카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.d(TAG, "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.d(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
                UserApiClient.instance.me { user, _ ->
                    val nickname = user?.kakaoAccount?.profile?.nickname.toString()
                    Toast.makeText(this, nickname + "님, 로그인을 환영합니다", Toast.LENGTH_LONG).show()
                }
                requestTokenValidation(token.accessToken, this) // 토큰 검증을 위해 백앤드 서버에 request
            }
        }
        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(applicationContext)) {
            UserApiClient.instance.loginWithKakaoTalk(applicationContext) { token, error ->
                if (error != null) {
                    Log.d(TAG, "카카오톡으로 로그인 실패", error)

                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }

                    // 카톡이 설치되어 있지만, 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(
                        applicationContext,
                        callback = callback
                    )
                } else if (token != null) {
                    Log.d(TAG, "카카오톡으로 로그인 성공 ${token.accessToken}")
                }
            }
        } else { // 카카오톡 설치가 안 되어있는 경우
            UserApiClient.instance.loginWithKakaoAccount(applicationContext, callback = callback)
        }
    }

    /**
    @description - 토큰 검증 함수로, 로그인 했을 때 받은 액세스 토큰과 현재 액티비티 객체를 받아온다.
    함수 내부에서 builder, baseurl 등 요청을 위한 정보를 담은 retrofit 객체를 정의하고, 서버에게 요청을 하기 위한 인터페이스 service 객체를 받아온다.
    service 객체를 통해 요청을 위한 메소드를 구현하고, 반환 값으로는 만들어둔 JsonElement 타입의 응답을 콜백으로 받아온다.
    신규 유저인지, 기존 유저인지 판단하여 DTO 객체를 맵핑하고 그 과정에서의 예외와 에러 메시지 처리 등을 포함하고 있다.
    @param - String(액세스 토큰), Activity(현재 액태비티를 받아온다)
    @return - Unit
    @author - Tae hyun Park
    @since - 2022-07-05 | 2022-07-21
     **/
    private fun requestTokenValidation(token: String, activity: Activity) {
        // 요청 인터페이스 구현을 위한 service 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val customProgressDialog = DialogUtil().ProgressDialog(activity) // 로딩창 progress 객체 생성
        customProgressDialog.showDialog() // 로딩 progress 시작

        // 토큰 요청과 응답이 이루어지는 콜백 함수
        service.requestKakaoLogin(System.getProperty("http.agent"), RequestKakaoToken(token))
            .enqueue(object : Callback<JsonElement> {
                // 응답이 왔으면
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    if (response.isSuccessful) {      // 토큰 검증에 대한 응답이 왔다면
                        try {
                            // 받아온 응답이 클라이언트의 요청 오류였다면
                            if (response.code() != 406 && response.code() >= 400) {
                                throw ResponseErrorException("\"요청에 실패하였습니다. code: ${response.code()}\\nerror: ${response.errorBody()}\"")
                            }
                            // 받아오는 응답 객체를 JSONObject 로 변환
                            val responseJson = JSONObject(response.body().toString())

                            // 받아온 응답이 에러에 대한 응답이라면
                            if (responseJson.has("errorMessage")) {
                                throw InvalidAccessTokenException("카카오 엑세스 토큰으로 인증할 수 없습니다. error:${responseJson["errorMessage"]}")
                            }

                            // 받아온 응답이 아래와 같이 정상적인 응답이라면 두 가지 경우로 DTO 할당
                            responseBody = if (responseJson.has("username")) {
                                ResponseLogin.convertJsonToObject(responseJson) // 신규 유저일 경우
                            } else {
                                ResponseExistLogin.convertJsonToObject(responseJson) // 기존 유저일 경우
                            }

                            // 정상적인 응답이었다면, JWT 토큰이 있는지 없는지를 검사하여 어떤 페이지로 넘겨줄 것인지 결정
                            if (response.code() in 200..299) {
                                val isJwtToken = JwtTokenUtil(applicationContext)

                                if (isJwtToken.hasJwtToken(
                                        ValueUtil.JWT_HEADER_KEY,
                                        response.headers()
                                    )
                                ) {  // 우선 응답으로 받아온 헤더에 JWT 토큰이 있을 경우
                                    if (customProgressDialog.isShowing()) // 로딩 progress 가 실행 중이라면
                                        customProgressDialog.dismissDialog() // 종료

                                    // 로컬에 JWT 토큰 새로 저장
                                    isJwtToken.setAccessToken(
                                        isJwtToken.getAccessTokenFromResponse(
                                            response.headers()
                                        )!!
                                    ) // 엑세스 토큰 저장
                                    isJwtToken.setRefreshToken(
                                        isJwtToken.getRefreshTokenFromResponse(response.headers())!!
                                    ) // 리프레시 토큰 저장

                                    // 기존 유저이므로 메인 페이지로 이동
                                    if (responseBody is ResponseExistLogin)
                                        moveToMainPage(responseBody as ResponseExistLogin)
                                    else
                                        showToast("정보를 불러오는데 실패하였습니다.")
                                } else { // 없다면 신규 유저이므로 회원 가입 페이지로 이동
                                    if (responseBody is ResponseLogin)
                                        moveToSignUpPage(responseBody as ResponseLogin)
                                    else
                                        showToast("정보를 불러오는데 실패하였습니다.")
                                }
                            } else {
                                // 응답 에러에 대한 예외 발생
                                throw ResponseErrorException(
                                    "요청에 실패하였습니다. code: ${response.code()}\nerror: ${
                                        response.errorBody()?.string()
                                    }"
                                )
                            }
                        } catch (e: ResponseErrorException) {
                            e.printStackTrace()
                            if (customProgressDialog.isShowing()) { // 로딩 progress 가 실행 중이라면
                                customProgressDialog.dismissDialog() // 종료
                            }
                            DialogUtil().SingleDialog(
                                this@SignInActivity,
                                "요청에 문제가 발생하였습니다.",
                                "확인"
                            ) {

                            }.show()
                        } catch (e: InvalidAccessTokenException) { // 엑세스 토큰을 인증할 수 없는 예외 처리
                            e.printStackTrace()
                            if (customProgressDialog.isShowing()) {
                                customProgressDialog.dismissDialog()
                            }
                            DialogUtil().SingleDialog(
                                this@SignInActivity,
                                "구글 인증에 문제가 발생하였습니다.",
                                "확인"
                            ) {

                            }.show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (customProgressDialog.isShowing()) {
                                customProgressDialog.dismissDialog()
                            }
                            DialogUtil().SingleDialog(
                                this@SignInActivity,
                                "예기치 못한 문제가 발생하였습니다.",
                                "확인"
                            ) {

                            }.show()
                        }
                    } else {
                        Log.d(TAG, "response error: " + response.errorBody()?.string()!!)
                    }
                }

                // 응답 받기를 실패했다면
                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    Log.d("onFailure", "실패$t")
                }
            })
    }

    /**
     * 회원가입 페이지(Activity) 로 이동하는 함수 with 데이터
     * @param response(ResponseLogin): 계정 유무 검증에 대한 응답 값
     * @return None
     * @author Seunggun Sin
     * @since 2022-07-09
     */
    private fun moveToSignUpPage(response: ResponseLogin) {
        val intent = Intent(this, SignUpActivity::class.java)
        intent.putExtra("responseBody", response) // Serializable class 데이터를 집어 넣음
        startActivity(intent)
    }

    /**
     * 기존 유저가 로그인 시 메인 페이지로 이동하는 함수 with 기본 유저 데이터
     * @param userInfo(ResponseExistLogin): 기존 유저에 대한 응답 값
     * @return None
     * @author Seunggun Sin
     * @since 2022-07-16
     */
    private fun moveToMainPage(userInfo: ResponseExistLogin) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("userInfo", userInfo)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    /**
     * 토스트 메세지를 보여주는 함수
     * @param message(String): 보여주고자 하는 메세지 문자열
     * @author Seunggun Sin
     * @since 2022-07-18
     */
    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}