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
import com.dope.breaking.exception.MissingJwtTokenException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.user.UserProfile
import com.dope.breaking.util.DialogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        CoroutineScope(Dispatchers.Main).launch {
            // 본인의 userId 값을 인자로 유저 프로필 정보 요청
            try {
                val userProfile = UserProfile(requireActivity())
                val user = userProfile.getUserProfileInfo(ResponseExistLogin.baseUserInfo!!.userId)

                // 핸들러를 통해 전달
                val bundle = Bundle()
                bundle.putSerializable("user", user)
                val message = handler.obtainMessage()
                message.data = bundle
                handler.sendMessage(message)
            } catch (e: ResponseErrorException) {
                DialogUtil().SingleDialog(
                    requireActivity(),
                    "정보를 불러오지 못했습니다. 재시도 바랍니다. ",
                    "확인"
                ) {
                    moveToHome()
                }.show()
            } catch (e: MissingJwtTokenException) {
                DialogUtil().SingleDialog(requireActivity(), "사용자를 식별할 수 없습니다! 앱을 재실행바랍니다.", "확인") {
                    moveToHome()
                }.show()
            } catch (e: Exception) {
                e.printStackTrace()
                DialogUtil().SingleDialog(requireActivity(), "예기치 못한 문제가 발생하였습니다.", "확인") {
                    moveToHome()
                }.show()
            }
        }
        return inflater.inflate(R.layout.progress_dialog_layout, container, false)
    }

    /**
     * 마이페이지 이동에 문제 발생 시, Home Fragment 로 이동
     * @author Seunggun Sin
     * @since 2022-07-30
     */
    private fun moveToHome() {
        val home = NaviHomeFragment()
        parentFragmentManager // Home Fragment 로 전환
            .beginTransaction()
            .replace(R.id.fl_board, home)
            .commit()
    }
}