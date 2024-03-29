package com.dope.breaking

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dope.breaking.databinding.ActivitySettingBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.signup.Account
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private val decimalFormat = DecimalFormat("#,###")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        binding.tbSetting.setNavigationIcon(R.drawable.ic_baseline_arrow_back_black_24)

        // 보유 금액 값 보여주기
        binding.tvFinanceValue.text =
            "${decimalFormat.format(ResponseExistLogin.baseUserInfo!!.balance)}원"

        binding.tbSetting.setNavigationOnClickListener {
            finish()
        }

        // 보유한 금액 레이아웃 클릭 시
        binding.clFinanceValue.setOnClickListener {
            startActivity(Intent(this@SettingActivity, FinanceActivity::class.java))
        }

        // 로그아웃 클릭 시
        binding.tvLogOut.setOnClickListener {
            DialogUtil().MultipleDialog(this@SettingActivity, "정말 로그아웃 하시겠습니까?", "예", "아니오", {
                CoroutineScope(Dispatchers.Main).launch {
                    val token =
                        ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(this@SettingActivity).getAccessTokenFromLocal()
                    try {
                        val result = Account().startRequestLogOut(token) // 로그아웃 요청

                        if (result) { // 성공했다면
                            ResponseExistLogin.baseUserInfo = null // 로그인 상태 초기화
                            moveToLoginPage() // 로그인 페이지로 이동
                        }
                    } catch (e: ResponseErrorException) {
                        DialogUtil().SingleDialog(
                            this@SettingActivity,
                            "로그아웃에 실패하였습니다. 재시도 바랍니다.",
                            "확인"
                        ).show()
                    }
                }
            }, {}, true).show()

        }

        setContentView(binding.root)
    }

    /**
     * 로그인 페이지로 이동 (이전 Activity Stack 모두 clear)
     * @author Seunggun Sin
     * @since 2022-08-20
     */
    private fun moveToLoginPage() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    /**
     * 보유 금액을 업데이트 하기 위한 생명주기 함수 충전, 출금 등으로 인한 계좌 값 반영
     */
    override fun onRestart() {
        super.onRestart()
        binding.tvFinanceValue.text =
            "${decimalFormat.format(ResponseExistLogin.baseUserInfo!!.balance)}원"
    }
}