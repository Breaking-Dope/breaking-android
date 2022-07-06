package com.dope.breaking

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dope.breaking.databinding.ActivitySignInBinding
import com.dope.breaking.model.KakaoLogin
import com.dope.breaking.model.RequestKakaoToken
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignInActivity: AppCompatActivity() {
    // Log Tag
    private val TAG = "SignInActivity.kt"

    // 전역 변수로 바인딩 객체 선언
    private var mbinding: ActivitySignInBinding? = null

    // 매번 null 체크할 필요 없이 바인딩 변수 재 선언
    private val binding get() = mbinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 액티비티에서 사용할 바인딩 클래스의 인스턴스 생성
        mbinding = ActivitySignInBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // 카카오 로그인 버튼 클릭 이벤트 메소드
        binding.btnSignInKakao.setOnClickListener(View.OnClickListener {
            loginKakao()
        })
    }

    /**
    @description - 카카오톡 어플의 설치 여부와 카카오계정의 유무에 따른 로그인 처리 함수 , 콜백함수에서 로그인에 대한 성공, 실패 결과 로직을 진행한다.
                   성공할 경우 OAuthToken 타입의 토큰을, 실패할 경우 Throwable 타입의 error 객체를 리턴한다.
    @param - None
    @return - Unit
    @author - Tae hyun Park
    @since - 2022-07-05
     **/
    fun loginKakao():Unit {
        // 카카오계정 로그인 공통 callback
        // 카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.d(TAG, "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.d(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
                UserApiClient.instance.me { user, error ->
                    var nickname = user?.kakaoAccount?.profile?.nickname.toString()
                    Toast.makeText(this,nickname+"님, 로그인을 환영합니다",Toast.LENGTH_LONG).show()
                }
                // 토큰 검증을 위해 retrofit을 이용하여 back-end 서버에 request
                ValidationLoginKakao(token.accessToken)

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
                    UserApiClient.instance.loginWithKakaoAccount(applicationContext, callback = callback)
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
    fun ValidationLoginKakao(token: String){
        // 요청 인터페이스 구현을 위한 service 객체 create
        var service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 요청 토큰 인스턴스 생성
        var requestTest = RequestKakaoToken(token)

        // 토큰 요청과 응답이 이루어지는 call-back 함수
        service.getKakaoUserInfo(requestTest).enqueue(object : Callback<KakaoLogin> {
            // 응답이 왔으면
            override fun onResponse(call: Call<KakaoLogin>, response: Response<KakaoLogin>) {
                if(response.isSuccessful) { // response의 body가 정상적인지
                    var data = response.body()    // GsonConverter를 사용해 데이터매핑하여 자동 변환
                    Log.d(TAG, "successful response body : "+data)
                }else{
                    Log.d(TAG, "response error: "+response.errorBody()?.string()!!)
                }
            }
            // 응답이 안 오는 경우
            override fun onFailure(call: Call<KakaoLogin>, t: Throwable) {
                Log.d("onFailure", "실패$t")
            }
        })
    }

}