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
    @SerializedName("isFollowing") val isFollowing: Boolean,
) : Serializable {
    companion object {
        /**
         * 응답으로 받아온 raw json 객체를 파싱하여 User 객체로 생성하는 함수
         * @param jsonObject(JSONObject): 응답 json 객체
         * @return User: 변환한 User 객체
         * @author Seunggun Sin
         * @since 2022-07-22 | 2022-08-02
         */
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
                if (jsonObject.isNull("isFollowing"))
                    jsonObject["following"] as Boolean
                else
                    jsonObject["isFollowing"] as Boolean
            )
        }
    }
}