package com.dope.breaking.model.request

import com.dope.breaking.model.PostLocation
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject

data class RequestPostData(
    val title: String,
    val content: String,
    val location: PostLocation,
    val price: Int,
    val hashtagList: ArrayList<String>,
    val postType: String,
    @get:JsonProperty("isAnonymous")
    val isAnonymous : Boolean,
    val eventTime : String,
    val thumbnailIndex : Int
){
    /**
     * 모든 필드 값들을 json 데이터로 변환해주는 메소드
     * @return JSONObject: 모든 필드들을 json 으로 변환한 객체 반환
     * @author Tae hyun Park
     * @since 2022-07-29 | 2022-08-02
     */
    fun convertFieldsToJson(): JSONObject {
        val jsonParentObject = JSONObject() // 부모
        val jsonChildObject = JSONObject()  // 자식 (Nested)

        jsonParentObject.put("title", title)
        jsonParentObject.put("content", content)

        jsonChildObject.put("region",location.region)
        jsonChildObject.put("latitude",location.latitude)
        jsonChildObject.put("longitude",location.longitude)
        jsonParentObject.put("location",jsonChildObject)

        jsonParentObject.put("price", price)
        jsonParentObject.put("hashtagList", hashtagList)
        jsonParentObject.put("postType", postType)
        jsonParentObject.put("isAnonymous", isAnonymous)
        jsonParentObject.put("eventTime", eventTime)
        jsonParentObject.put("thumbnailIndex", thumbnailIndex)
        return jsonParentObject
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