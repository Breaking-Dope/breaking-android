package com.dope.breaking.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitManager {
    // 레트로핏 컴패니언 객체 선언
    companion object{
        val retrofit = Retrofit.Builder()
            .baseUrl("https://team-dope.link:8443") // 서버 주소
            .addConverterFactory(GsonConverterFactory.create()) // gson 컨버터
            .build()

        // 구글 api 서버에 대한 retrofit 객체 선언
        val retrofitGoogle = Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 카카오 api 서버에 대한 retrofit 객체 선언
        val retrofitKakao = Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }
}