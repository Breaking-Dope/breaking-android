package com.dope.breaking.retrofit

import com.dope.breaking.model.FollowData
import com.dope.breaking.model.request.RequestGoogleAccessToken
import com.dope.breaking.model.request.RequestGoogleToken
import com.dope.breaking.model.request.RequestKakaoToken
import com.dope.breaking.model.response.*
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
    @return - Call<JsonElement>
    @author - Tae hyun Park
    @since - 2022-07-05 | 2022-07-20
     **/
    @POST("oauth2/sign-in/kakao")
    fun requestKakaoLogin(
        @Header("User-Agent") userAgent: String,
        @Body tokens: RequestKakaoToken
    ): Call<JsonElement>

    /**
     * 회원가입 시 전화번호 검증 요청 메소드
     * @request - String
     * @response - Unit
     * @author - Tae hyun Park
     */
    @GET("/oauth2/sign-up/validate-phone-number/{phoneNumber}")
    suspend fun requestValidationPhoneNum(
        @Header("authorization") token: String,
        @Path("phoneNumber") phoneNumber: String
    ): Response<Unit>

    /**
     * 회원가입 시 닉네임 검증 요청 메소드
     * @request - String
     * @response - Unit
     * @author - Tae hyun Park
     */
    @GET("/oauth2/sign-up/validate-nickname/{nickName}")
    suspend fun requestValidationNickName(
        @Header("authorization") token: String,
        @Path("nickName") nickName: String
    ): Response<Unit>

    /**
     * 회원가입 시 이메일 검증 요청 메소드
     * @request - String
     * @response - Unit
     * @author - Tae hyun Park
     */
    @GET("/oauth2/sign-up/validate-email/{email}")
    suspend fun requestValidationEmail(
        @Header("authorization") token: String,
        @Path("email") email: String
    ): Response<Unit>

    /**
     * 제보하기 게시글 업로드 요청 메소드
     * @Part - mediaList(ArrayList<MultipartBody.Part>) : 제보 미디어 리스트
     * @Part - data(RequestBody) : 제보 게시글 데이터
     * @author - Tae hyun Park
     */
    @Multipart
    @POST("/post")
    suspend fun requestPostUpload(
        @Header("authorization") token: String,
        @Part mediaList: ArrayList<MultipartBody.Part>?,
        @Part("data") data: RequestBody
    ): Response<ResponsePostUpload>

    /**
     * 카카오톡 지도로부터 검색 키워드에 대한 위치정보를 가져오는 메소드
     * @Query - query(String) : 검색 키워드
     * @Query - page(Int) : 결과 페이지 번호
     * @author - Tae hyun Park
     */
    @GET("v2/local/search/keyword.json")
    fun getSearchKeyword(
        @Header("authorization") key: String,     // 카카오 Rest API 인증키 [필수]
        @Query("query") query: String,            // 검색을 원하는 질의어 [필수]
        @Query("page") page: Int                  // 결과 페이지 번호 [옵션]
    ): Call<ResponseLocationSearch>

    /**
     * 구글 로그인 토큰 검증 요청 메소드
     * @param userAgent: 안드로이드 플랫폼 user-agent 헤더 값
     * @request RequestGoogleToken
     * @response ResponseLogin
     * @author Seunggun Sin
     */
    @POST("oauth2/sign-in/google")
    fun requestGoogleLogin(
        @Header("User-Agent") userAgent: String,
        @Body tokens: RequestGoogleToken
    ): Call<JsonElement>

    /**
     * 최종 회원가입 요청 메소드 - Multipart 요청
     * @param userAgent: 안드로이드 플랫폼 user-agent 헤더 값
     * @param image(MultipartBody.Part): 이미지가 담긴 multi form 요청 body (key=profileImg)
     * @param data(RequestBody): 나머지 텍스트 필드  값이 담긴 요청 body (key=signUpRequest)
     * @response Unit: 응답 body 자체는 중요 x, Jwt 토큰을 위한 헤더 값만 필요
     * @author Seunggun Sin
     */
    @Multipart
    @POST("oauth2/sign-up")
    suspend fun requestSignUp(
        @Header("User-Agent") userAgent: String,
        @Part image: MultipartBody.Part,
        @Part("signUpRequest") data: RequestBody
    ): Response<Unit>

    /**
     * 앱을 재시작하고 자동 로그인 시, Jwt 토큰 만료 확인을 위한 요청 메소드
     * @param token(String): "authorization" 헤더 키로 Jwt 토큰 값을 넣음
     * @response ResponseJwtUserInfo (기본 유저 정보에 대한 DTO 클래스)
     * @author Seunggun Sin
     */
    @GET("oauth2/validate-jwt")
    suspend fun requestValidationJwt(@Header("authorization") token: String): Response<ResponseExistLogin>

    /**
     * 엑세스 토큰 만료 시, 토큰을 재발급하기 위한 요청
     * @param userAgent: 안드로이드 플랫폼 user-agent 헤더 값
     * @param accessToken: 로컬에 저장된 엑세스 토큰
     * @param refreshToken: 로컬에 저장된 리프레시 토큰
     * @response: 성공 시 body 없음. 에러 발생 시 json element로 받음
     */
    @GET("reissue")
    suspend fun requestReissueJwtToken(
        @Header("User-Agent") userAgent: String,
        @Header("authorization") accessToken: String,
        @Header("authorization-refresh") refreshToken: String
    ): Response<JsonElement>

    /**
     * 메인 피드 리스트를 가져오는 요청
     * @param token(String): Jwt 토큰 - 헤더 (옵션)
     * @param lastPostId(Int): 마지막으로 가져온 post id (필수) (처음 요청 시에는 0 or null)
     * @param contentsSize(Int): 가져올 게시글 개수 (필수)
     * @param sortCategory(String?): 정렬 - 정렬 카테고리 (옵션)
     * @param soldOption(String?): 필터 - 판매 상태 (옵션)
     * @param dateFrom(String?): 필터 - 시작 날짜 (옵션)
     * @param dateTo(String?): 필터 - 종료 날짜 (옵션)
     * @param latestMin(Int?): 필터 - 최근 N분 (옵션)
     * @response ResponseMainFeed 리스트
     * @author Seunggun Sin
     */
    @GET("feed")
    suspend fun requestGetMainFeed(
        @Header("authorization") token: String = "",
        @Query("cursor") lastPostId: Int,
        @Query("size") contentsSize: Int,
        @Query("sort") sortCategory: String? = null,
        @Query("sold-option") soldOption: String? = null,
        @Query("date-from") dateFrom: String? = null,
        @Query("date-to") dateTo: String? = null,
        @Query("for-last-min") latestMin: Int? = null
    ): Response<List<ResponseMainFeed>>

    /**
     * 유저의 고유 id 를 갖고 유저의 프로필 정보를 가져오는 요청 (회원가입이 되어있는 유저가 요청하는 경우)
     * @path userId: 유저 고유 id 값
     * @header authorization: 요청하는 유저의 Jwt 토큰 값
     * @response Json 으로 요청하고자하는 해당 유저의 프로필 정보 값
     * @author Seunggun sin
     */
    @GET("profile/{userId}")
    suspend fun requestUserProfileInfo(
        @Path("userId") userId: Long,
        @Header("authorization") token: String
    ): Response<JsonElement>

    /**
     * 유저 정보 변경을 위한 본인의 유저 데이터를 가져오는 요청
     * @header authorization: 요청하는 유저의 Jwt 토큰 값
     * @response DetailUser: 기존 유저의 정보 DTO 객체
     * @author Seunggun Sin
     */
    @GET("profile/detail")
    fun requestDetailUserInfo(@Header("authorization") token: String): Call<DetailUser>

    /**
     * Multipart 유저 프로필 변경 요청
     * @header authorization: 요청하는 유저의 Jwt 토큰 값
     * @param image(MultipartBody.Part?): 이미지 데이터 (null 값 보낼 시 기본 이미지로 변경)
     * @param data(RequestBody): 요청 데이터 필드 값
     * @response x
     * @author Seunggun Sin
     */
    @Multipart
    @PUT("profile")
    suspend fun requestUpdateUserInfo(
        @Header("authorization") token: String,
        @Part image: MultipartBody.Part?,
        @Part("updateRequest") data: RequestBody
    ): Response<Unit>

    /**
     * userId 에 해당하는 사람의 팔로잉 리스트를 가져오는 요청
     * @header authorization: 요청하는 유저의 Jwt 토큰 값
     * @path userId: 팔로잉 리스트를 얻고자 하는 유저의 고유 아이디
     * @query cursor: 마지막으로 요청한 마지막 id
     * @query size: 팔로우 리스트에서 가져올 아이템 개수
     * @response userId 에 해당하는 사람이 팔로우한 사람들의 리스트
     * @author Seunggun Sin
     */
    @GET("follow/following/{userId}")
    suspend fun requestGetFollowingList(
        @Header("authorization") token: String,
        @Path("userId") userId: Long,
        @Query("cursor") cursorId: Int,
        @Query("size") contentSize: Int
    ): Response<List<FollowData>>

    /**
     * userId 에 해당하는 사람의 팔로워 리스트를 가져오는 요청
     * @header authorization: 요청하는 유저의 Jwt 토큰 값
     * @path userId: 팔로워 리스트를 얻고자 하는 유저의 고유 아이디
     * @query cursor: 마지막으로 요청한 마지막 id
     * @query size: 팔로우 리스트에서 가져올 아이템 개수
     * @response userId 에 해당하는 사람을 팔로워한 사람들의 리스트
     * @author Seunggun Sin
     */
    @GET("follow/follower/{userId}")
    suspend fun requestGetFollowerList(
        @Header("authorization") token: String,
        @Path("userId") userId: Long,
        @Query("cursor") cursorId: Int,
        @Query("size") contentSize: Int
    ): Response<List<FollowData>>

    /**
     * userId 에 해당하는 사람에게 팔로우를 요청
     * @header authorization: 요청하는 본인을 식별하기 위한 Jwt 토큰(필수)
     * @path userId: 팔로우 요청하고자 하는 대상 Id
     * @response x
     * @author Seunggun Sin
     */
    @POST("follow/{userId}")
    suspend fun requestFollow(
        @Header("authorization") token: String,
        @Path("userId") userId: Long
    ): Response<Unit>

    /**
     * userId 에 해당하는 사람에게 언팔로우를 요청
     * @header authorization: 요청하는 본인을 식별하기 위한 Jwt 토큰(필수(
     * @path userId: 언팔로우 요청하고자 하는 대상 Id
     * @response x
     * @author Seunggun Sin
     */
    @DELETE("follow/{userId}")
    suspend fun requestUnFollow(
        @Header("authorization") token: String,
        @Path("userId") userId: Long
    ): Response<Unit>

    /**
     * OAuth2 구글 API 서버로부터 엑세스 토큰 요청 메소드
     * @request RequestGoogleAccessToken
     * @response ResponseGoogleAccessToken
     * @author Seunggun Sin
     */
    @POST("token")
    suspend fun requestGoogleAccessToken(@Body googleRequest: RequestGoogleAccessToken): Response<ResponseGoogleAccessToken>
}