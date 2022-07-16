package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.Serializable

data class ResponseExistLogin(
    @SerializedName("userId") val userId: Int,
    @SerializedName("profileImgURL") val profileImgUrl: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("balance") val balance: Int
) : Serializable {
    companion object {
        fun convertJsonToObject(jsonObject: JSONObject): ResponseExistLogin {
            return ResponseExistLogin(
                jsonObject["userId"] as Int,
                jsonObject["profileImgURL"] as String,
                jsonObject["nickname"] as String,
                jsonObject["balance"] as Int
            )
        }
    }
}