package com.dope.breaking.retrofit

import com.dope.breaking.model.response.User
import com.dope.breaking.model.request.RequestGoogleAccessToken
import com.dope.breaking.model.request.RequestGoogleToken
import com.dope.breaking.model.request.RequestKakaoToken
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.ResponseGoogleAccessToken
import com.dope.breaking.model.response.ResponseLogin
import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    suspend fun requestValidationPhoneNum(@Path("phoneNumber") phoneNumber: String): Response<Unit>

    /**
     * 회원가입 시 닉네임 검증 요청 메소드
     * @request - String
     * @response - Unit
     * @author - Tae hyun Park
     */
    @GET("/oauth2/sign-up/validate-nickname/{nickName}")
    suspend fun requestValidationNickName(@Path("nickName") nickName: String): Response<Unit>

    /**
     * 회원가입 시 이메일 검증 요청 메소드
     * @request - String
     * @response - Unit
     * @author - Tae hyun Park
     */
    @GET("/oauth2/sign-up/validate-email/{email}")
    suspend fun requestValidationEmail(@Path("email") email: String): Response<Unit>

    /**
     * 구글 로그인 토큰 검증 요청 메소드
     * @request RequestGoogleToken
     * @response ResponseLogin
     * @author Seunggun Sin
     */
    @POST("oauth2/sign-in/google")
    fun requestGoogleLogin(@Body tokens: RequestGoogleToken): Call<JsonElement>

    /**
     * 최종 회원가입 요청 메소드 - Multipart 요청
     * @param image(MultipartBody.Part): 이미지가 담긴 multi form 요청 body (key=profileImg)
     * @param data(RequestBody): 나머지 텍스트 필드  값이 담긴 요청 body (key=signUpRequest)
     * @response Unit: 응답 body 자체는 중요 x, Jwt 토큰을 위한 헤더 값만 필요
     * @author Seunggun Sin
     */
    @Multipart
    @POST("oauth2/sign-up")
    suspend fun requestSignUp(
        @Part image: MultipartBody.Part,
        @Part("signUpRequest") data: RequestBody
    ): Response<Unit>

    /**
     * 앱을 재시작하고 자동 로그인 시, Jwt 토큰 만료 확인을 위한 요청 메소드
     * @param token(String): "authorization" 헤더 키로 Jwt 토큰 값을 넣음
     * @response ResponseJwtUserInfo (기본 유저 정보에 대한 DTO 클래스)
     * @author Seunggun Sin
     */
    @POST("oauth2/validate-jwt")
    suspend fun requestValidationJwt(@Header("Authorization") token: String): Response<ResponseExistLogin>

    /**
     * 유저의 고유 id 를 갖고 유저의 프로필 정보를 가져오는 요청 (회원가입이 되어있는 유저가 요청하는 경우)
     * @path userId: 유저 고유 id 값
     * @header authorization: 요청하는 유저의 Jwt 토큰 값
     * @response User 객체로 요청하고자하는 해당 유저의 프로필 정보
     * @author Seunggun sin
     */
    @GET("profile/{userId}")
    fun requestUserProfileInfo(
        @Path("userId") userId: Long,
        @Header("authorization") token: String
    ): Call<User>

    /**
     * OAuth2 구글 API 서버로부터 엑세스 토큰 요청 메소드
     * @request RequestGoogleAccessToken
     * @response ResponseGoogleAccessToken
     * @author Seunggun Sin
     */
    @POST("token")
    suspend fun requestGoogleAccessToken(@Body googleRequest: RequestGoogleAccessToken): Response<ResponseGoogleAccessToken>
}