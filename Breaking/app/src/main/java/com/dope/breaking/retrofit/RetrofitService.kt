package com.dope.breaking.retrofit

import com.dope.breaking.model.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface RetrofitService {
    /**
    @description - 카카오 로그인 토큰 검증 요청 메소드
    @param - RequestKakaoToken
    @return - Call<KakaoLogin>
    @author - Tae hyun Park
    @since - 2022-07-05 | 2022-07-06
     **/
    @POST("oauth2/sign-in/kakao")
    fun requestKakaoLogin(@Body token: RequestKakaoToken): Call<ResponseLogin>

    /**
     * 회원가입 시 전화번호 검증 요청 메소드
     * @request - String
     * @response - Unit
     * @author - Tae hyun Park
     */
    @GET("/oauth2/sign-up/validate-phone-number/{phoneNumber}")
    suspend fun requestValidationPhoneNum(@Path("phoneNumber") phoneNumber: String) : Response<Unit>

    /**
     * 회원가입 시 닉네임 검증 요청 메소드
     * @request - String
     * @response - Unit
     * @author - Tae hyun Park
     */
    @GET("/oauth2/sign-up/validate-nickname/{nickName}")
    suspend fun requestValidationNickName(@Path("nickName") nickName: String) : Response<Unit>

    /**
     * 회원가입 시 이메일 검증 요청 메소드
     * @request - String
     * @response - Unit
     * @author - Tae hyun Park
     */
    @GET("/oauth2/sign-up/validate-email/{email}")
    suspend fun requestValidationEmail(@Path("email") email: String) : Response<Unit>

    /**
     * 구글 로그인 토큰 검증 요청 메소드
     * @request - RequestGoogleToken
     * @response - ResponseLogin
     * @author - Seunggun Sin
     */
    @POST("oauth/sign-in/google")
    fun requestGoogleLogin(@Body tokens: RequestGoogleToken): Call<ResponseLogin>

    /**
     * OAuth2 구글 API 서버로부터 엑세스 토큰 요청 메소드
     * @request - RequestGoogleAccessToken
     * @response - ResponseGoogleAccessToken
     * @author - Seunggun Sin
     */
    @POST("token")
    fun requestGoogleAccessToken(@Body googleRequest: RequestGoogleAccessToken): Call<ResponseGoogleAccessToken>
}