package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DetailUser(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("realName") val realName: String,
    @SerializedName("role") val role: String,
    @SerializedName("statusMsg") val statusMsg: String,
    @SerializedName("profileImgURL") val profileImgURL: String?,
): Serializable