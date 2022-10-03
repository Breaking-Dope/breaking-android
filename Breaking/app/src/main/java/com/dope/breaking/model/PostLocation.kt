package com.dope.breaking.model

import org.json.JSONObject
import java.io.Serializable

data class PostLocation(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val region_1depth_name: String,
    val region_2depth_name: String
) : Serializable {
    /**
     * 모든 필드 값들을 json 데이터로 변환해주는 메소드
     * @return JSONObject: 모든 필드들을 json 으로 변환한 객체 반환
     * @author Tae hyun Park
     * @since 2022-07-29 | 2022-08-09
     */
    fun convertFieldsToJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("address", address)
        jsonObject.put("latitude", latitude)
        jsonObject.put("longitude", longitude)
        jsonObject.put("region_1depth_name", region_1depth_name)
        jsonObject.put("region_2depth_name", region_2depth_name)
        return jsonObject
    }

    /**
     * json 형태의 데이터를 json string 형태로 변환해주는 메소드
     * @return String: 필드 값들을 json 형태로 변환 후 뒤, 문자열로 변환한 값 반환
     * @author Tae hyun Park
     * @since 2022-07-29
     */
    fun convertJsonToString(): String {
        val json = convertFieldsToJson() // json 으로 변환
        return json.toString() // json 을 문자열로 변환
    }
}