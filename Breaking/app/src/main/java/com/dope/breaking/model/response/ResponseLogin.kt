package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.Serializable

data class ResponseLogin(
    @SerializedName("fullname") var fullName: String, // 유저 성 이름
    @SerializedName("username") var userName: String, // 회원번호
    @SerializedName("email") var userEmail: String,   // 유저 이메일
    @SerializedName("profileImgURL") var profileImgUrl: String? // 프로필 이미지 Url
) : Serializable {
    companion object {
        fun convertJsonToObject(jsonObject: JSONObject): ResponseLogin {
            return ResponseLogin(
                jsonObject["fullname"] as String,
                jsonObject["username"] as String,
                jsonObject["email"] as String,
                if (jsonObject.isNull("profileImgURL")) null else jsonObject["profileImgURL"] as String?
            )
        }
    }
}
