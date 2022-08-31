package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName

data class ResponseUserSearch(
    @SerializedName("userId") val userId: Long,
    @SerializedName("profileImgURL") val profileImgURL: String?,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("statusMsg") val statusMsg: String,
    @SerializedName("followerCount") val followerCount: Long,
    @SerializedName("isFollowing") var isFollowing: Boolean,
)