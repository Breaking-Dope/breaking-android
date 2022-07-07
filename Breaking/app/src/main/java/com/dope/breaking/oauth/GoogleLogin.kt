package com.dope.breaking.oauth

import android.content.Context
import com.dope.breaking.BuildConfig
import com.dope.breaking.model.RequestGoogleAccessToken
import com.dope.breaking.model.RequestGoogleToken
import com.dope.breaking.model.ResponseGoogleAccessToken
import com.dope.breaking.model.ResponseLogin
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.*
import okhttp3.Headers
import retrofit2.Response

class GoogleLogin(private val context: Context) {
    private val jwtHeaderKey = "authorization" // JWT 토큰 검증을 위한 헤더 키 값

    // 구글 로그인 시 여러가지 옵션에 대한 객체
    private val googleSignInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.clientId) // id 토큰을 요청하는 옵션
            .requestServerAuthCode(BuildConfig.clientId) // 서버의 인가코드를 요청하는 옵션
            .requestEmail() // 사용자의 이메일을 요청하는 옵션
            .build()

    val googleSignInClient: GoogleSignInClient =
        GoogleSignIn.getClient(this.context, googleSignInOptions) // 구글 로그인을 수행하는 클라이언트 객체


    suspend fun requestGoogleLogin(account: GoogleSignInAccount): Boolean {
        return CoroutineScope(Dispatchers.Main).async(Dispatchers.IO){ // 코루틴 실행
            /*
            구글의 엑세스 토큰을 얻기 위해 구글 api 서버에 요청하는 메소드 호출
            프로젝트에 저장된 클라이언트 id와 비밀번호, 그리고 로그인 시 받은 계정에 존재하는 서버 인가 코드를 인자로 넘긴다.
            만약 결과가 null 이라면 중단한다.
            */
            val accessTokenResponseObject = getGoogleAccessToken(
                BuildConfig.clientId,
                BuildConfig.clientSecret,
                account.serverAuthCode ?: ""
            ) ?: return@async false

            /*
            구글로부터 받아온 id 토큰과 access 토큰으로 백엔드 서버에 로그인을 요청하는 메소드 호출
            */
            val validationResponse = requestTokenValidation(
                accessTokenResponseObject.idToken,
                accessTokenResponseObject.accessToken
            )
            return@async hasJwtToken(jwtHeaderKey, validationResponse.headers())
        }.await()
    }

    /**
     * oauth2 구글 api 서버에 엑세스 토큰을 얻기 위한 요청을 보내는 작업 수행
     * 요청 자체는 동기적으로 실행되며 코루틴을 사용하여 응답의 결과를 async/await 로 전달한다.
     * suspend 함수
     * @param clientId(String): 구글 로그인에 대한 우리 프로젝트의 클라이언트 ID
     * @param clientSecret(String): 구글 로그인에 대한 우리 프로젝트의 클라이언트 보안 비밀번호
     * @param code(String): 사용자에 의한 구글 로그인 요청으로 받은 서버 인가 코드 값 (serverAuthCode)
     * @return - ResponseGoogleAccessToken? 객체로 id 토큰과 access 토큰 필드가 담겨 있다.
     * @author - Seunggun Sin
     * @since - 2022-07-07
     */
    private suspend fun getGoogleAccessToken(
        clientId: String,
        clientSecret: String,
        code: String
    ): ResponseGoogleAccessToken? {
        if (code.isEmpty()) { // 서버 인가 코드가 없는 값이라면
            return null // 요청을 거부하고 null 리턴
        }
        /* 코루틴 실행 */
        return CoroutineScope(Dispatchers.Main).async(Dispatchers.IO) {
            val googleRetrofitService =
                RetrofitManager.retrofitGoogle.create(RetrofitService::class.java) // 구글 retrofit 서비스 객체 생성

            // 구글 api 서버에 엑세스 토큰을 동기적으로 요청 → .execute() 메소드
            val res = googleRetrofitService.requestGoogleAccessToken(
                RequestGoogleAccessToken(
                    client_id = clientId,
                    client_secret = clientSecret,
                    code = code
                )
            ).execute()
            return@async res.body() // 응답의 body 를 리턴
        }.await() // async 블럭의 코드가 실행될 때까지 기다리고 끝나면 결과 값 리턴
    }

    /**
     * 구글로부터 받아온 id 토큰과 access 토큰을 바탕으로 백엔드 서버에 로그인을 요청한다. (토큰 검증)
     * @param idToken(String): ID 토큰
     * @param accessToken(String): Access 토큰
     * @return - Response<ResponseLogin> 으로 응답 raw 데이터 반환
     * @author Seunggun Sin
     * @since - 2022-07-07
     */
    private suspend fun requestTokenValidation(
        idToken: String,
        accessToken: String
    ): Response<ResponseLogin> {
        return CoroutineScope(Dispatchers.Main).async(Dispatchers.IO) {
            // 백엔드 서버에 대한 retrofit 서비스 객체 생성
            val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

            /*
            parameter 로 받아온 값을 바탕으로 객체를 요청 body 로 전달 후 백엔드 서버에 로그인 요청
            동기적으로 요청 → .execute() 메소드
            */
            return@async service.requestGoogleLogin(
                RequestGoogleToken(
                    idToken,
                    accessToken
                )
            ).execute() // 응답 결과 자체를 리턴
        }.await() // async 블럭의 코드가 실행될 때까지 기다리고 끝나면 결과 값 리턴
    }

    /**
     * JWT 토큰이 있는지 없는지 판단. 헤더의 값이 null 이거나 빈 문자열이면 false 리턴, 값이 존재하면 true 리턴.
     * @param headerName(String): jwt 토큰을 위한 헤더 key 값으로, "authorization"로 고정
     * @param headers(Headers): 백엔드 서버로 요청의 결과로 받은 응답의 헤더 객체인 response.headers().
     * @return 응답의 결과로 토큰이 있는지 없는지에 대한 bool 값 리턴
     * @author - Seunggun Sin
     * @since - 2022-07-07
     */
    private fun hasJwtToken(headerName: String, headers: Headers): Boolean =
    // Map 데이터로 인덱스 연산자에 문자열을 넣는 key-value 방식을 사용하여 헤더 map 데이터에 headerName 문자열을 넣었을 때
        // 나오는 value 의 값이 null 이거나 빈 문자열이라면 false 리턴, 그렇지 않고 정상적인 값이 있으면 true 리턴
        !(headers[headerName] == null || headers[headerName]!!.isEmpty())
}