package com.dope.breaking.fragment.finance_tab

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dope.breaking.databinding.FragmentFinanceChargeBinding
import com.dope.breaking.finance.FinanceManager
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class ChargeTabFragment : Fragment() {
    private lateinit var binding: FragmentFinanceChargeBinding
    private val decimalFormat = DecimalFormat("#,###") // 숫자 포맷팅
    private var inputPrice: String = "" // 마지막으로 입력한 숫자 문자열
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFinanceChargeBinding.inflate(inflater, container, false)

        // 충전하기 입력창 텍스트 감지
        val textWatcher = object : TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString().isNotEmpty() && p0.toString() != inputPrice) {
                    inputPrice = decimalFormat.format(p0.toString().replace(",", "").toInt())
                    binding.etInputCharge.setText(inputPrice)
                    binding.etInputCharge.setSelection(inputPrice.length)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        }
        binding.etInputCharge.addTextChangedListener(textWatcher)

        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(requireContext()).getAccessTokenFromLocal()

        // 충전하기 버튼 클릭 시 이벤트
        binding.btnCharge.setOnClickListener {
            // 입력한 값이 있다면
            if (binding.etInputCharge.text.toString().isNotEmpty())
            // 충전하기 재확인 dialog 실행
                DialogUtil().MultipleDialog(
                    requireContext(),
                    "  정말로 ${inputPrice}원을 충전하시겠습니까?  ",
                    "충전하기",
                    "뒤로가기",
                    leftEvent = {
                        CoroutineScope(Dispatchers.Main).launch {
                            val result = FinanceManager().startDepositRequest(
                                token,
                                getDecimalPrice(binding.etInputCharge.text.toString())
                            ) // 입력한 값만큼 입금 요청

                            if (result) { // 충전하기 성공 시
                                ResponseExistLogin.baseUserInfo!!.balance += getDecimalPrice(binding.etInputCharge.text.toString()) // 유저 상태 정보의 계좌 업데이트
                                showToast("충전이 완료되었습니다.")
                                requireActivity().finish()
                            } else // 충전하기 실패 시
                                showToast("충전에 실패하였습니다.")
                        }
                    },
                    allowCancel = true
                ).show()
        }
        return binding.root
    }

    /**
     * 포맷화된 숫자 문자열을 실제 숫자 값으로 반환하는 함수
     * @param format(String): 포맷팅된 숫자 문자열
     * @return Int: 변환한 실제 (Integer) 숫자
     * @author Seunggun Sin
     * @since 2022-09-02
     */
    private fun getDecimalPrice(format: String): Int {
        return format.replace(",", "").toInt()
    }

    /**
     * 토스트 메세지를 보여주는 함수
     * @param message(String): 보여주고자 하는 문자열
     * @author Seunggun Sin
     * @since 2022-09-02
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}