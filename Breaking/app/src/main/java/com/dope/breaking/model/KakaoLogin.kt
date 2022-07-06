package com.dope.breaking.model

import com.google.gson.annotations.SerializedName

data class KakaoLogin (
    @SerializedName("fullname") var fullName:String, // 유저 성 이름
    @SerializedName("username") var userName:String, // 회원번호
    @SerializedName("email") var userEmail:String,   // 유저 이메일
    @SerializedName("profileImgURL") var profileImgUrl:String // 프로필 이미지 Url
)
