package com.dope.breaking.retrofit

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class RetrofitManager {
    // 레트로핏 컴패니언 객체 선언

    companion object {
        // 영상 처리의 경우 서버와 통신이 오래 걸리므로 소켓 timeOutException 을 방지하고자 가능한 최대 통신 소요 시간을 늘리도록 정의.
        var okHttpClient: OkHttpClient? = OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.MINUTES)
            .readTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(100, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://team-dope.link:8443") // 서버 주소
            .addConverterFactory(GsonConverterFactory.create()) // gson 컨버터
            .client(okHttpClient)
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