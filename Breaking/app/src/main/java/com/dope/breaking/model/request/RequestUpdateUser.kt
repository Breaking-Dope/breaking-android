package com.dope.breaking.model.request

import org.json.JSONObject

data class RequestUpdateUser(
    val nickname: String,
    val phoneNumber: String,
    val email: String,
    val realName: String,
    val role: String,
    val statusMsg: String,
    val isProfileImgChanged: Boolean
) {
    /**
     * 모든 필드 값들을 json 데이터로 변환해주는 메소드
     * @return JSONObject: 모든 필드들을 json 으로 변환한 객체 반환
     * @author Seunggun Sin
     * @since 2022-07-25 | 2022-08-01
     */
    fun convertFieldsToJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("nickname", nickname)
        jsonObject.put("phoneNumber", phoneNumber)
        jsonObject.put("email", email)
        jsonObject.put("realName", realName)
        jsonObject.put("role", role)
        jsonObject.put("statusMsg", statusMsg)
        jsonObject.put("isProfileImgChanged", isProfileImgChanged)
        return jsonObject
    }

    /**
     * json 형태의 데이터를 json string 형태로 변환해주는 메소드
     * @return String: 필드 값들을 json 형태로 변환 후 뒤, 문자열로 변환한 값 반환
     * @author Seunggun Sin
     * @since 2022-07-25
     */
    fun convertJsonToString(): String {
        val json = convertFieldsToJson() // json 으로 변환
        return json.toString() // json 을 문자열로 변환
    }
}