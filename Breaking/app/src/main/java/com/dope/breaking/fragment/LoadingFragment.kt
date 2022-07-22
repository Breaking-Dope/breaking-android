package com.dope.breaking.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dope.breaking.R
import com.dope.breaking.model.response.User
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.JwtTokenUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 * 요청을 담당하는 Fragment 로 중간다리 역할 (여기서는 로딩 progress dialog 를 보여줌)
 * 요청의 결과 값을 받아와서 결과를 보여주는 Fragment 로 전달해서 깔끔하게 보여주도록 함
 */
class LoadingFragment : Fragment() {
    private val handler: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            val pass = NaviUserFragment() // 유저 Fragment 객체 생성
            pass.arguments = msg.data // Fragment 의 인자 값에 받아온 유저 데이터 저장
            parentFragmentManager // 유저 Fragment 로 전환
                .beginTransaction()
                .replace(R.id.fl_board, pass)
                .commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        val tokenUtil = JwtTokenUtil(requireContext()) // Jwt 토큰 객체 생성

        // 유저 Id와 로컬에 저장된 본인의 Jwt 토큰을 인자로 전달 후 유저 프로필 정보 요청
        service.requestUserProfileInfo(
            ResponseExistLogin.baseUserInfo!!.userId, tokenUtil.getTokenFromLocal()!!
        ).enqueue(object :
            Callback<User?> {
            override fun onResponse(call: Call<User?>, response: Response<User?>) {
                if (response.isSuccessful) {
                    val user = response.body()!! // 유저 응답 객체 가져오기

                    // 핸들러를 통해 전달
                    val bundle = Bundle()
                    bundle.putSerializable("user", user)
                    val message = handler.obtainMessage()
                    message.data = bundle
                    handler.sendMessage(message)

                }
            }

            override fun onFailure(call: Call<User?>, t: Throwable) {
            }
        })
        return inflater.inflate(R.layout.progress_dialog_layout, container, false)
    }

}