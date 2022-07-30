package com.dope.breaking.user

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.widget.ImageView
import com.dope.breaking.EditProfileActivity
import com.dope.breaking.ImageExpansionActivity
import com.dope.breaking.UserPageActivity
import com.dope.breaking.exception.MissingJwtTokenException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.DetailUser
import com.dope.breaking.model.response.User
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserProfile(private val activity: Activity) {
    /**
     * 프로필 편집 버튼 클릭 시 프로필 편집 페이지로 이동하는 함수
     * @param detailUserData(DetailUser): 프로필 편집을 위한 기존 유저의 데이터 클래스
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    fun moveToEditProfile(detailUserData: DetailUser) {
        val intent = Intent(activity, EditProfileActivity::class.java)
        intent.putExtra("detail", detailUserData)
        activity.startActivity(intent)
    }

    /**
     * 프로필 이미지 클릭 시, 이미지 확대 페이지로 이동 (Transition Animation 적용)
     * @param url(String): 현재 프로필 페이지에 보여지고 있는 이미지에 대한 url
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    fun moveToExpandedProfile(url: String, imageView: ImageView) {
        val intent = Intent(activity, ImageExpansionActivity::class.java)
        intent.putExtra("imgUrl", url)
        val opt = ActivityOptions.makeSceneTransitionAnimation( // Transition Animation
            activity,
            imageView,
            "imgTrans" // 지정된 transition 이름에 해당하는 View 를 기준
        )
        activity.startActivity(intent, opt.toBundle())
    }

    /**
     * userId 에 해당하는 사람의 프로필 페이지로 이동
     * @param userId(Long): 프로필을 보여주고자 하는 대상의 id
     * @author Seunggun Sin
     * @since 2022-07-30
     */
    fun moveToUserPage(userId: Long) {
        val intent = Intent(activity, UserPageActivity::class.java)
        intent.putExtra("userId", userId)
        activity.startActivity(intent)
    }

    /**
     * 현재 로그인한 유저의 Jwt 토큰을 이용하여 프로필 편집에 필요한 기존 유저 데이터를 요청하는 함수
     * 동시에 요청 성공 시 프로필 편집 페이지로 이동한다.
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    fun getUserDetailInfo() {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val token = JwtTokenUtil(activity).getTokenFromLocal() // 로컬에 저장된 토큰 가져오기
        if (token.isNotEmpty())
        // 기존 유저 데이터 요청
            service.requestDetailUserInfo(ValueUtil.JWT_REQUEST_PREFIX + token)
                .enqueue(object : Callback<DetailUser?> {
                    override fun onResponse(
                        call: Call<DetailUser?>,
                        response: Response<DetailUser?>
                    ) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null) // 성공하고 정상적인 값이 존재하면
                                moveToEditProfile(body) // 프로필 편집 페이지로 이동
                        } else {
                            DialogUtil().SingleDialog(
                                activity,
                                "정보를 불러오지 못했습니다. 재시도 바랍니다. ",
                                "확인"
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<DetailUser?>, t: Throwable) {
                        DialogUtil().SingleDialog(
                            activity,
                            "서버에 문제가 발생하였습니다. 재시도 바랍니다.",
                            "확인"
                        ).show()
                    }
                })
        else
            DialogUtil().SingleDialog(activity, "사용자를 식별할 수 없습니다! 앱을 재실행바랍니다.", "확인").show()
    }

    /**
     * userId 에 해당하는 유저의 정보를 불러오는 요청
     * @param userId(Long): 불러오고자 하는 대상의 고유 id
     * @return User: 응답으로 받아온 해당 유저의 User DTO 객체
     * @throws ResponseErrorException: 정상 응답이 아닌 경우에 대한 예외 처리
     * @author Seunggun Sin
     * @since 2022-07-29
     */
    @Throws(ResponseErrorException::class)
    suspend fun getUserProfileInfo(userId: Long): User {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val token = JwtTokenUtil(activity).getTokenFromLocal() // 로컬에 저장된 토큰 가져오기

        if (token.isNotEmpty()) {
            val response =
                service.requestUserProfileInfo(userId, JwtTokenUtil(activity).getTokenFromLocal())
            if (response.code() in 200..299) {
                return response.body()!!
            } else {
                throw ResponseErrorException("응답 에러")
            }
        } else {
            throw MissingJwtTokenException("토큰이 없습니다.")
        }
    }
}