package com.dope.breaking.oauth

import android.content.Context
import com.dope.breaking.BuildConfig
import com.dope.breaking.exception.InvalidAccessTokenException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.request.RequestGoogleAccessToken
import com.dope.breaking.model.request.RequestGoogleToken
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.ResponseGoogleAccessToken
import com.dope.breaking.model.response.ResponseLogin
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.gson.JsonElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.json.JSONObject
import retrofit2.Response

class GoogleLogin(private val context: Context) {
    var responseBody: Any? = null // 구글 로그인 프로세스 가운데 얻는 response body 값

    // 구글 로그인 시 여러가지 옵션에 대한 객체
    private val googleSignInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.clientId) // id 토큰을 요청하는 옵션
            .requestServerAuthCode(BuildConfig.clientId) // 서버의 인가코드를 요청하는 옵션
            .requestEmail() // 사용자의 이메일을 요청하는 옵션
            .build()

    val googleSignInClient: GoogleSignInClient =
        GoogleSignIn.getClient(this.context, googleSignInOptions) // 구글 로그인을 수행하는 클라이언트 객체

    /**
     * OAuth2 구글 로그인 프로세스 과정에 대한 함수 (엑세스 토큰 요청 → 토큰 검증 요청 → Jwt 토큰 유무 체크)
     * @param account(GoogleSignInAccount): 구글 로그인 시 얻는 계정 정보 객체
     * @return Boolean: 구글 로그인 과정을 통해 사용자가 회원가입이 필요한지 필요하지 않은지에 대한 bool 값 리턴(필요하면 false 리턴)
     * @throws ResponseErrorException: 정상 응답 (2xx) 이외의 응답이 왔을 때 exception 발생
     * @throws InvalidAccessTokenException: 에러 응답(406), 즉, 플랫폼에 인증할 수 없을 때 발생하는 응답에 대한 예외
     * @author Seunggun Sin
     * @since 2022-07-07 | 2022-07-28
     */
    @Throws(ResponseErrorException::class, InvalidAccessTokenException::class)
    suspend fun requestGoogleLogin(account: GoogleSignInAccount): Boolean {
        /*
        구글의 엑세스 토큰을 얻기 위해 구글 api 서버에 요청하는 메소드 호출
        프로젝트에 저장된 클라이언트 id와 비밀번호, 그리고 로그인 시 받은 계정에 존재하는 서버 인가 코드를 인자로 넘긴다.
        만약 결과가 null 이라면 중단한다.
        */
        val accessTokenResponseObject = getGoogleAccessToken(
            BuildConfig.clientId,
            BuildConfig.clientSecret,
            account.serverAuthCode ?: ""
        ) ?: return false

        /*
        구글로부터 받아온 id 토큰과 access 토큰으로 백엔드 서버에 로그인을 요청하는 메소드 호출
        */
        try {
            val validationResponse = requestTokenValidation(
                accessTokenResponseObject.idToken,
                accessTokenResponseObject.accessToken
            )
            if (validationResponse?.code() in 200..299) {
                // 토큰이 있으면 true, 없으면 false 리턴
                val jwtTokenUtil = JwtTokenUtil(context)
                return if (jwtTokenUtil.hasJwtToken(
                        ValueUtil.JWT_HEADER_KEY,
                        validationResponse!!.headers()
                    )
                ) {
                    // 로컬에 저장된 토큰이 없다면 저장하기
                    if (jwtTokenUtil.getAccessTokenFromLocal() == "") {
                        jwtTokenUtil.setAccessToken(
                            jwtTokenUtil.getAccessTokenFromResponse(validationResponse.headers())!!
                        ) // 엑세스 토큰 로컬에 저장
                        jwtTokenUtil.setRefreshToken(
                            jwtTokenUtil.getRefreshTokenFromResponse(validationResponse.headers())!!
                        ) // 리프레시 토큰 로컬에 저장
                    }
                    true
                } else
                    false
            } else {
                // 응답 에러에 대한 예외 발생
                throw ResponseErrorException(
                    "요청에 실패하였습니다. code: ${validationResponse!!.code()}\nerror: ${
                        validationResponse.errorBody()?.string()
                    }"
                )
            }
        } catch (e: InvalidAccessTokenException) {
            // 토큰으로 인증할 수 없는 에러에 대한 예외 발생 처리
            throw InvalidAccessTokenException(e.message!!)
        } catch (e: ResponseErrorException) {
            // 예기치 못한 응답 에러에 대한 예외 발생
            throw ResponseErrorException(e.message!!)
        }
    }

    /**
     * OAuth2 구글 api 서버에 엑세스 토큰을 얻기 위한 요청을 보내는 작업 수행
     * retrofit 요청 메소드의 리턴을 Response 로 설정하여 값을 바로 받아오도록 수정
     * suspend 함수
     * @param clientId(String): 구글 로그인에 대한 우리 프로젝트의 클라이언트 ID
     * @param clientSecret(String): 구글 로그인에 대한 우리 프로젝트의 클라이언트 보안 비밀번호
     * @param code(String): 사용자에 의한 구글 로그인 요청으로 받은 서버 인가 코드 값 (serverAuthCode)
     * @return ResponseGoogleAccessToken? 객체로 id 토큰과 access 토큰 필드가 담겨 있다.
     * @author Seunggun Sin
     * @since 2022-07-07 | 2022-07-10
     */
    private suspend fun getGoogleAccessToken(
        clientId: String,
        clientSecret: String,
        code: String
    ): ResponseGoogleAccessToken? {
        if (code.isEmpty()) { // 서버 인가 코드가 없는 값이라면
            return null // 요청을 거부하고 null 리턴
        }
        val googleRetrofitService =
            RetrofitManager.retrofitGoogle.create(RetrofitService::class.java) // 구글 retrofit 서비스 객체 생성

        // 구글 api 서버에 엑세스 토큰을 동기적으로 요청 → .execute() 메소드
        val res = googleRetrofitService.requestGoogleAccessToken(
            RequestGoogleAccessToken(
                client_id = clientId,
                client_secret = clientSecret,
                code = code
            )
        )
        return res.body() // 응답의 body 를 리턴
    }

    /**
     * 구글로부터 받아온 id 토큰과 access 토큰을 바탕으로 백엔드 서버에 로그인을 요청한다. (토큰 검증)
     * retrofit 요청 메소드의 리턴을 Response 로 설정하여 값을 바로 받아오도록 수정
     * suspend 함수
     * ※ 응답의 형태가 여러가지인 경우 Response 방식이 아닌 Call 방식 사용하여 코드에 따른 처리하기
     * @param idToken(String): ID 토큰
     * @param accessToken(String): Access 토큰
     * @throws ResponseErrorException: 서버에서 지정한 에러 응답이 아닌 경우에 발생하는 예외
     * @throws InvalidAccessTokenException: 서버에서 토큰을 통해 유저를 인증할 수 없을 때 발생하는 예외
     * @return Response<JsonElement>? 으로 응답 그 자체를 반환
     * @author Seunggun Sin
     * @since 2022-07-07 | 2022-07-16
     */
    @Throws(ResponseErrorException::class, InvalidAccessTokenException::class)
    private suspend fun requestTokenValidation(
        idToken: String,
        accessToken: String
    ): Response<JsonElement>? {
        return CoroutineScope(Dispatchers.Main).async(Dispatchers.IO) {
            // 백엔드 서버에 대한 retrofit 서비스 객체 생성
            val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
            /*
            parameter 로 받아온 값을 바탕으로 객체를 요청 body 로 전달 후 백엔드 서버에 로그인 요청
            동기적으로 요청 → .execute() 메소드
            */
            val response =
                service.requestGoogleLogin(
                    System.getProperty("http.agent"),
                    RequestGoogleToken(
                        idToken,
                        accessToken
                    )
                ).execute()
            // 먼저 예기치 못한 응답 코드에 대해서 예외 발생 시키기
            if (response.code() != 406 && response.code() >= 400) {
                throw ResponseErrorException(
                    "요청에 실패하였습니다. code: ${response.code()}\nerror: ${
                        response.errorBody()?.string()
                    }"
                )
            }
            // 응답으로 온 json string 을 json object 로 변환
            val responseJson = JSONObject(response.body().toString())

            // 서버에서 지정된 에러에 대한 응답 처리
            if (responseJson.has("errorMessage")) {
                throw InvalidAccessTokenException("구글 엑세스 토큰으로 인증할 수 없습니다. error:${responseJson["errorMessage"]}")
            }

            // 정상 응답에 대한 body 를 구분하여 DTO 저장
            responseBody = if (responseJson.has("username")) {
                ResponseLogin.convertJsonToObject(responseJson) // 신규 유저
            } else {
                ResponseExistLogin.convertJsonToObject(responseJson) // 기존 유저
            }
            return@async response // 응답 결과 자체를 리턴
        }.await() // async 블럭의 코드가 실행될 때까지 기다리고 끝나면 결과 값 리턴
    }
}