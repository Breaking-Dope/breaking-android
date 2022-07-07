package com.dope.breaking.model

import com.google.gson.annotations.SerializedName

data class ResponseGoogleAccessToken(
    @SerializedName("access_token") var accessToken: String, // 구글 엑세스 토큰
    @SerializedName("expires_in") var expiresIn: String, // 엑세스 토큰의 만료까지 남은 시간(초)
    @SerializedName("id_token") var idToken: String,   // 사용자 ID 토큰
    @SerializedName("refresh_token") var refreshToken: String, // 새 엑세스 토큰을 얻는 사용되는 토큰
    @SerializedName("scope") var scope: String, // 엑세스 토큰에서 부여하는 엑세스 범위
    @SerializedName("token_type") var tokenType: String // 반환된 토큰의 타입 - 항상 Bearer 설정됨
)