package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.Serializable

data class User(
    @SerializedName("userId") val userId: Long,
    @SerializedName("profileImgURL") val profileImgURL: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("statusMsg") val statusMsg: String,
    @SerializedName("followerCount") val followerCount: Int,
    @SerializedName("followingCount") val followingCount: Int,
//    @SerializedName("postCount") val postCount: Long,
    @SerializedName("following") val following: Boolean,
) : Serializable {
    companion object {
        fun convertJsonToObject(jsonObject: JSONObject): User {
            return User(
                (jsonObject["userId"] as Int).toLong(),
                if (jsonObject.isNull("profileImgURL")) "" else jsonObject["profileImgURL"] as String,
                jsonObject["nickname"] as String,
                jsonObject["email"] as String,
                jsonObject["role"] as String,
                jsonObject["statusMsg"] as String,
                jsonObject["followerCount"] as Int,
                jsonObject["followingCount"] as Int,
//                jsonObject["postCount"] as Long,
                jsonObject["following"] as Boolean
            )
        }
    }
}