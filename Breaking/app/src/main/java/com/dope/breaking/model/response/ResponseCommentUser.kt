package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ResponseCommentUser(
    @SerializedName("userId") val userId: Long, // 유저 고유 번호
    @SerializedName("profileImgURL") val profileImgUrl: String, // 프로필 이미지 url
    @SerializedName("nickname") val nickname: String, // 유저 닉네임
) : Serializable
