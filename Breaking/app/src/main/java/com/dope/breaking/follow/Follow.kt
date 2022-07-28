package com.dope.breaking.follow

import android.content.Context
import android.content.Intent
import com.dope.breaking.FollowActivity
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.FollowData
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService

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
     * @param token(String): 요청하는 유저의 Jwt 토큰 (없으면 비회원 유저)
     * @param userId(Long): 요청 대상의 고유 id
     * @return List<FollowData>: userId 가 팔로우한 사람들에 대한 리스트
     * @throws ResponseErrorException: 응답 코드가 2xx 가 아닌 응답에 대한 예외 처리
     * @author Seunggun Sin
     * @since 2022-07-28
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetFollowingList(token: String, userId: Long): List<FollowData> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val response = service.requestGetFollowingList(token, userId)

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
     * @param token(String): 요청하는 유저의 Jwt 토큰 (없으면 비회원 유저)
     * @param userId(Long): 요청 대상의 고유 id
     * @return List<FollowData>: userId 를 팔로워한 사람들에 대한 리스트
     * @throws ResponseErrorException: 응답 코드가 2xx 가 아닌 응답에 대한 예외 처리
     * @author Seunggun Sin
     * @since 2022-07-28
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetFollowerList(token: String, userId: Long): List<FollowData> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val response = service.requestGetFollowerList(token, userId)

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

    companion object {
        /**
         * 팔로우(워) 페이지로 이동
         * @param context(Context): 페이지 이동에 대한 caller context
         * @param state(Boolean): 팔로우 리스트(true)인지 팔로워 리스트(false)인지 구분
         * @param userId(Long): 팔로우(워) 페이지 대상의 고유 id
         */
        fun moveToFollowInfo(context: Context, state: Boolean, userId: Long) {
            val intent = Intent(context, FollowActivity::class.java)
            intent.putExtra("state", state)
            intent.putExtra("userId", userId)
            context.startActivity(intent)
        }
    }
}