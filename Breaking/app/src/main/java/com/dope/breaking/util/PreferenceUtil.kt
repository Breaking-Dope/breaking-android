package com.dope.breaking.util

import android.content.Context

/**
 * SharedPreferences 를 간단하게 사용하기 위한 util 클래스
 * 필요에 따라 다양한 메소드 추가
 * @param context(Context): 현재 컨텍스트
 * @param name(String): SharedPreferences 에 저장할 파일 이름
 */
class PreferenceUtil(context: Context, name: String) {
    // preference 객체 생성
    private val pref = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    /**
     * key 에 대한 문자열 value 반환
     * @param key(String): value 값을 가져오기 위한 key 값
     * @param default(String): key 에 대한 value 값이 없을 때 default 로 넘겨주는 value.
     * @return - key 에 대응되는 value 값 리턴, key 에 대해 저장된 값이 없으면 default 리턴.
     * @author - Seunggun Sin
     * @since - 2022-07-08
     */
    fun getString(key: String, default: String): String {
        return pref.getString(key, default).toString()
    }

    /**
     * key 에 대한 문자열 value 를 저장
     * @param key(String): key 값
     * @param value(String): 문자열 value 값
     * @return - None
     * @author - Seunggun Sin
     * @since - 2022-07-08
     */
    fun setString(key: String, value: String) {
        pref.edit().putString(key, value).apply()
    }
}