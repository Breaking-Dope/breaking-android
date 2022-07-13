package com.dope.breaking

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dope.breaking.databinding.ActivitySignInBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.*
import com.dope.breaking.oauth.GoogleLogin
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.JwtTokenUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignInActivity : AppCompatActivity() {
    // Log Tag
    private val TAG = "SignInActivity.kt"

    private var mbinding: ActivitySignInBinding? = null  // 전역 변수로 바인딩 객체 선언

    private val binding get() = mbinding!!     // 매번 null 체크할 필요 없이 바인딩 변수 재 선언

    private val jwtHeaderKey = "authorization" // JWT 토큰 검증을 위한 헤더 키 값

    //구글 로그인 intent 호출 후, 로그인 intent 의 결과를 받기 위해 ActivityResult 객체 선언
    private lateinit var googleLoginActivityResult: ActivityResultLauncher<Intent>

    private lateinit var googleLogin: GoogleLogin // 구글 로그인에 필요한 기능들이 담겨있는 커스텀 클래스 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 액티비티에서 사용할 바인딩 클래스의 인스턴스 생성
        mbinding = ActivitySignInBinding.inflate(layoutInflater)

        setContentView(binding.root)

        googleLogin = GoogleLogin(this) // 구글 로그인 커스텀 클래스 객체 초기화

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
                        CoroutineScope(Dispatchers.Main).launch {
                            /*
                            받아온 계정 데이터를 구글 로그인 요청 메소드의 인자로 넘겨줌으로써 토큰 검증 프로세스 시작
                            메소드의 결과로 회원가입이 필요한지 필요하지 않은지 boolean 값을 리턴(jwt 토큰의 유무)
                            true 면 회원가입 필요, false 면 회원가입 필요 x
                            */
                            val isExisting = googleLogin.requestGoogleLogin(account)
                            // Jwt 토큰이 없고, response body 가 정상적인 값이 있다면

                            if (!isExisting && googleLogin.responseBody != null) {
                                // 회원가입 페이지로 이동
                                moveToSignUpPage(googleLogin.responseBody!!)
                            } else {
                                // 로그인 처리와 메인 피드로 이동 + 에러 처리 필요
                            }
                        }
                    } catch (e: ApiException) { // ApiException: 구글 로그인 시 발생하는 에러
                        e.printStackTrace()
                    } catch (e: ResponseErrorException) {
                        e.printStackTrace()
                        // 응답 에러에 대한 예외 처리하기
                    }
                }
            }
        // 구글 로그인 버튼 클릭 이벤트
        binding.btnSignInGoogle.setOnClickListener {
            startGoogleLoginIntent()
        }

        // 카카오 로그인 버튼 클릭 이벤트 메소드
        binding.btnSignInKakao.setOnClickListener(View.OnClickListener {
            loginKakao()
        })
    }

    /**
     * 구글 로그인에 대한 intent 를 호출한다. 로그인 intent 에 대한 결과를 받기 위해 activityResult 사용
     * @param - None
     * @return - Unit
     * @author - Seunggun Sin
     * @since - 2022-07-07
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
    @since - 2022-07-05
     **/
    fun loginKakao(): Unit {
        // 카카오계정 로그인 공통 callback
        // 카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.d(TAG, "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.d(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
                UserApiClient.instance.me { user, error ->
                    var nickname = user?.kakaoAccount?.profile?.nickname.toString()
                    Toast.makeText(this, nickname + "님, 로그인을 환영합니다", Toast.LENGTH_LONG).show()
                }
                // 토큰 검증을 위해 retrofit을 이용하여 back-end 서버에 request
                validationLoginKakao(token.accessToken)

                // 로그인 성공 시 넘어가는 로직 작성 필요
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
    @description - 토큰 검증 함수로, 로그인 했을 때 받은 accessToken 을 매개변수로 받아온다.
    함수 내부에서 builder, baseurl 등 요청을 위한 정보를 담은 retrofit 객체를 정의하고, 서버에게 요청을 하기 위한 인터페이스 service 객체를 받아온다.
    service 객체를 통해 요청을 위한 메소드를 구현하고, 반환 값으로는 만들어둔 response body 타입의 call-back 을 구현한다.
    그 과정에서 응답이 성공적인지, 실패인지를 판단한다.
    @param - token: String
    @return - Unit
    @author - Tae hyun Park
    @since - 2022-07-05 | 2022-07-06
     **/
    fun validationLoginKakao(token: String) {
        // 요청 인터페이스 구현을 위한 service 객체 create
        var service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 요청 토큰 인스턴스 생성
        var requestTest = RequestKakaoToken(token)

        // 토큰 요청과 응답이 이루어지는 call-back 함수
        service.requestKakaoLogin(requestTest).enqueue(object : Callback<ResponseLogin> {
            // 응답이 왔으면
            override fun onResponse(call: Call<ResponseLogin>, response: Response<ResponseLogin>) {
                if (response.isSuccessful) { // response의 body가 정상적인지
                    var data = response.body()    // GsonConverter를 사용해 데이터매핑하여 자동 변환
                    Log.d(TAG, "successful response body : " + data)

                    var jwtTokenUtil = JwtTokenUtil(applicationContext)
                    if(!jwtTokenUtil.hasJwtToken(jwtHeaderKey, response.headers())){
                        if (data != null)
                            moveToSignUpPage(data)
                    }
                } else {
                    Log.d(TAG, "response error: " + response.errorBody()?.string()!!)
                }
            }

            // 응답이 안 오는 경우
            override fun onFailure(call: Call<ResponseLogin>, t: Throwable) {
                Log.d("onFailure", "실패$t")
            }
        })
    }

    /**
     * 회원가입 페이지(Activity) 로 이동하는 함수 with 데이터
     * @param response(ResponseLogin): 계정 유무 검증에 대한 응답 값
     * @return - None
     * @author - Seunggun Sin
     * @since - 2022-07-09
     */
    private fun moveToSignUpPage(response: ResponseLogin) {
        val intent = Intent(this, SignUpActivity::class.java)
        intent.putExtra("responseBody", response) // Serializable class 데이터를 집어 넣음
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this) // 이전에 구글 로그인을 했던 계정 가져오기
        if (account != null) { // 계정이 있다면
            Log.d("already", account.email ?: "")
            Log.d("already2", account.idToken ?: "")
            Log.d("already3", account.givenName ?: "")
            Log.d("already4", account.familyName ?: "")
            Log.d("already5", account.displayName ?: "")
            Log.d("already5", account.serverAuthCode ?: "")
        }
    }
}