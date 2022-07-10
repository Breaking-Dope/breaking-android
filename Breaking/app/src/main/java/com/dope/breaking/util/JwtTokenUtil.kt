package com.dope.breaking.util

import android.content.Context
import okhttp3.Headers

/**
 * Jwt 토큰과 관련된 처리를 하는 Util 클래스
 * 필요에 따라 다양한 메소드 추가
 * @param context(Context): 현재 컨텍스트
 */
class JwtTokenUtil(context: Context) {
    private val fileName = "jwt" // SharedPreferences 의 파일 이름
    private val keyName = "authorization" // 헤더 키 값
    private val prefUtil = PreferenceUtil(context, fileName) // SharedPreferences Util 객체

    /**
     * 로컬 파일, 즉, SharedPreferences 로부터 토큰 값을 가져오는 메소드
     * @param - None
     * @return String?: 로컬에 저장된 jwt 토큰 값 리턴, 없다면 null 리턴
     * @author - Seunggun Sin
     * @since - 2022-07-09
     */
    fun getTokenFromLocal(): String? {
        val value = prefUtil.getString("token", "none")
        return if (value == "none") null else value
    }

    /**
     * Http 요청의 응답의 헤더로부터 Jwt 토큰 값을 가져오는 메소드
     * @param headers(Headers): 응답 헤더 객체
     * @return String?: 헤더에 담긴 jwt 토큰 값 리턴, 없다면 null 리턴
     * @author - Seunggun Sin
     * @since - 2022-07-09
     */
    fun getTokenFromResponse(headers: Headers): String? {
        return headers[keyName]
    }

    /**
     * 로컬에 토큰 값을 저장하는 메소드 (key=token)
     * @param token(String): 저장하고자 하는 jwt 토큰 문자열
     * @return - None
     * @author - Seunggun Sin
     * @since - 2022-07-09
     */
    fun setToken(token: String) {
        prefUtil.setString("token", token)
    }

    /**
     * JWT 토큰이 있는지 없는지 판단. 헤더의 값이 null 이거나 빈 문자열이면 false 리턴, 값이 존재하면 true 리턴.
     * @param headerName(String): jwt 토큰을 위한 헤더 key 값으로, "authorization"로 고정
     * @param headers(Headers): 백엔드 서버로 요청의 결과로 받은 응답의 헤더 객체인 response.headers().
     * @return 응답의 결과로 토큰이 있는지 없는지에 대한 bool 값 리턴
     * @author - Seunggun Sin
     * @since - 2022-07-07
     */
    fun hasJwtToken(headerName: String, headers: Headers): Boolean =
    // Map 데이터로 인덱스 연산자에 문자열을 넣는 key-value 방식을 사용하여 헤더 map 데이터에 headerName 문자열을 넣었을 때
        // 나오는 value 의 값이 null 이거나 빈 문자열이라면 false 리턴, 그렇지 않고 정상적인 값이 있으면 true 리턴
        !(headers[headerName] == null || headers[headerName]!!.isEmpty())

}