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
    }
}