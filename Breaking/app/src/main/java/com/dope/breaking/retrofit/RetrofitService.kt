package com.dope.breaking.retrofit

import com.dope.breaking.model.*
import retrofit2.Call
import retrofit2.http.*

interface RetrofitService {
    /**
    @description - 카카오 로그인 토큰 검증 요청 메소드
    @param - @Body token : RequestKakaoToken
    @return - Call<KakaoLogin>
    @author - Tae hyun Park
    @since - 2022-07-05 | 2022-07-06
     **/
    @POST("oauth/sign-in/kakao")
    fun requestKakaoLogin(@Body token: RequestKakaoToken): Call<ResponseLogin>

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