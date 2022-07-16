package com.dope.breaking.model.request

import org.json.JSONObject

/**
 * 회원가입 요청을 보낼 때 넣는 데이터 클래스
 */
data class RequestSignUp(
    val username: String,
    val nickname: String,
    val phoneNumber: String,
    val email: String,
    val realName: String,
    val statusMsg: String,
    private val _role: Boolean // 신분 필드: 일반인=true, 언론인=false
) {
    val role: String = if (_role) "USER" else "PRESS" // Boolean 에서 문자열로 변환

    /**
     * 모든 필드 값들을 json 데이터로 변환해주는 메소드
     * @return JSONObject: 모든 필드들을 json 으로 변환한 객체 반환
     * @author Seunggun Sin
     * @since 2022-07-08
     */
    fun convertFieldsToJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("username", username)
        jsonObject.put("nickname", nickname)
        jsonObject.put("phoneNumber", phoneNumber)
        jsonObject.put("email", email)
        jsonObject.put("realName", realName)
        jsonObject.put("statusMsg", statusMsg)
        jsonObject.put("role", role)
        return jsonObject
    }

    /**
     * json 형태의 데이터를 json string 형태로 변환해주는 메소드
     * @return String: 필드 값들을 json 형태로 변환 후 뒤, 문자열로 변환한 값 반환
     * @author Seunggun Sin
     * @since 2022-07-08
     */
    fun convertJsonToString(): String {
        val json = convertFieldsToJson() // json 으로 변환
        return json.toString() // json 을 문자열로 변환
    }

}
