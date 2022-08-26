package com.dope.breaking.model.response
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ResponsePostWriter(
    @SerializedName("userId") val userId: Long, // 유저 고유 번호
    @SerializedName("profileImgURL") val profileImgUrl: String, // 프로필 이미지 url
    @SerializedName("nickname") val nickname: String, // 유저 닉네임
    @SerializedName("phoneNumber") val phoneNumber: String // 유저 핸드폰 번호
) : Serializable
