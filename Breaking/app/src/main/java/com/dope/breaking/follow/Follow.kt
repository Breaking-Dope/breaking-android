package com.dope.breaking.follow

import android.content.Context
import android.content.Intent
import com.dope.breaking.FollowActivity
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.exception.UnLoginAccessException
import com.dope.breaking.model.FollowData
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.ValueUtil
import com.google.gson.JsonObject
import org.json.JSONObject

/**
 * 팔로우 관련 처리를 하는 클래스
 * 1. 팔로잉 리스트 가져오기
 * 2. 팔로워 리스트 가져오기
 * 3. 팔로우 하기
 * 4. 언팔로우 하기
 * 5. 팔로우 페이지 이동
 **/
class Follow {

    /**
     * userId 에 해당하는 사람의 팔로우 목록을 가져오는 요청
     * @param cursorId(Int): 마지막으로 가져온 리스트의 마지막 인덱스 값(리스트의 인덱스)
     * @param token(String): 요청하는 유저의 Jwt 토큰 (없으면 비회원 유저)
     * @param userId(Long): 요청 대상의 고유 id
     * @return List<FollowData>: userId 가 팔로우한 사람들에 대한 리스트
     * @throws ResponseErrorException: 응답 코드가 2xx 가 아닌 응답에 대한 예외 처리
     * @author Seunggun Sin
     * @since 2022-07-28 | 2022-08-18
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetFollowingList(
        cursorId: Int,
        token: String,
        userId: Long
    ): List<FollowData> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val response =
            service.requestGetFollowingList(token, userId, cursorId, ValueUtil.FOLLOW_SIZE)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null)
                return body
            else
                throw ResponseErrorException("정보를 불러오지 못했습니다.")
        } else {
            throw ResponseErrorException("응답 에러")
        }
    }

    /**
     * userId 에 해당하는 사람의 팔로워 목록을 가져오는 요청
     * @param cursorId(Int): 마지막으로 가져온 리스트의 마지막 인덱스 값(리스트의 인덱스)
     * @param token(String): 요청하는 유저의 Jwt 토큰 (없으면 비회원 유저)
     * @param userId(Long): 요청 대상의 고유 id
     * @return List<FollowData>: userId 를 팔로워한 사람들에 대한 리스트
     * @throws ResponseErrorException: 응답 코드가 2xx 가 아닌 응답에 대한 예외 처리
     * @author Seunggun Sin
     * @since 2022-07-28 | 2022-08-18
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetFollowerList(cursorId: Int, token: String, userId: Long): List<FollowData> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val response =
            service.requestGetFollowerList(token, userId, cursorId, ValueUtil.FOLLOW_SIZE)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null)
                return body
            else
                throw ResponseErrorException("정보를 불러오지 못했습니다.")
        } else {
            throw ResponseErrorException("응답 에러")
        }
    }

    /**
     * userId 에 해당하는 유저를 팔로우하는 요청 (Jwt 토큰 필수)
     * @param token(String): 본인 식별을 위한 Jwt 토큰
     * @param userId(Long): 팔로우 하고자 하는 대상의 id
     * @return Boolean: 팔로우 성공 시 true, 실패 시 false
     * @throws ResponseErrorException: 서버 에러 코드(5xx) 응답 시 예외 발생
     * @throws UnLoginAccessException: 비로그인 상태로 팔로우 시도 시 예외 발생(로그인 필수)
     * @author Seunggun Sin
     * @since 2022-07-30 | 2022-09-14
     */
    @Throws(ResponseErrorException::class, UnLoginAccessException::class)
    suspend fun startFollowRequest(token: String, userId: Long): Boolean {
        if (token.length < 8) { // Bearer 접두사 이후로 값이 없으면 Jwt 토큰 없다고 간주
            throw UnLoginAccessException("로그인이 필요합니다.")
        }
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        val response = service.requestFollow(token, userId) // 팔로우 요청

        if (response.isSuccessful) {
            return response.code() in 200..299
        } else {
            val errorString = response.errorBody()?.string()!!
            val errorJson = JSONObject(errorString)
            if (errorJson.get("code").toString() == "BSE401" || errorJson.get("code")
                    .toString() == "BSE000"
            )
                throw UnLoginAccessException(errorJson.toString())
            throw ResponseErrorException(errorString)
        }
    }

    /**
     * userId 에 해당하는 유저를 언팔로우하는 요청 (Jwt 토큰 필수)
     * @param token(String): 본인 식별을 위한 Jwt 토큰
     * @param userId(Long): 언팔로우 하고자 하는 대상의 id
     * @return Boolean: 언팔로우 성공 시 true, 실패 시 false
     * @throws ResponseErrorException: 서버 에러 코드(5xx) 응답 시 예외 발생
     * @throws UnLoginAccessException: 비로그인 상태로 팔로우 시도 시 예외 발생(로그인 필수)
     * @author Seunggun Sin
     * @since 2022-07-30 | 2022-09-14
     */
    @Throws(ResponseErrorException::class, UnLoginAccessException::class)
    suspend fun startUnFollowRequest(token: String, userId: Long): Boolean {
        if (token.length < 8) { // Bearer 접두사 이후로 값이 없으면 Jwt 토큰 없다고 간주
            throw UnLoginAccessException("로그인이 필요합니다.")
        }
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        val response = service.requestUnFollow(token, userId) // 언팔로우 요청

        if (response.isSuccessful) {
            return response.code() in 200..299
        } else {
            val errorString = response.errorBody()?.string()!!
            val errorJson = JSONObject(errorString)
            if (errorJson.get("code").toString() == "BSE401" || errorJson.get("code")
                    .toString() == "BSE000"
            )
                throw UnLoginAccessException(errorJson.toString())
            throw ResponseErrorException(errorString)
        }
    }

    companion object {
        /**
         * 팔로우(워) 페이지로 이동
         * @param context(Context): 페이지 이동에 대한 caller context
         * @param state(Boolean): 팔로우 리스트(true)인지 팔로워 리스트(false)인지 구분
         * @param userId(Long): 팔로우(워) 페이지 대상의 고유 id
         * @author Seunggun Sin
         * @since 2022-07-29
         */
        fun moveToFollowInfo(context: Context, state: Boolean, userId: Long) {
            val intent = Intent(context, FollowActivity::class.java)
            intent.putExtra("state", state)
            intent.putExtra("userId", userId)
            context.startActivity(intent)
        }
    }
}