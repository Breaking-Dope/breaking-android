package com.dope.breaking.retrofit

import com.dope.breaking.model.RequestKakaoToken
import com.dope.breaking.model.KakaoLogin
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
    fun getKakaoUserInfo(
        @Body token : RequestKakaoToken
    ) : Call<KakaoLogin>
}