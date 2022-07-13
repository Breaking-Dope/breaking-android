package com.dope.breaking

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import com.dope.breaking.databinding.ActivitySignUpBinding
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import retrofit2.Response

class Validation() {
    private val TAG = "Validation.kt" // Log Tag

    /**
    @description - 회원가입 요청 전에 닉네임, 전화번호, 이메일의 유효성의 검증을 요청하는 동기적 함수이다. 추가적으로 이름 필드가 빈 문자열인지도 검사한다.
    응답 에러 케이스의 경우 에러 코드가 아직 미구현이므로 String 으로 판별하였음.
    @param - String, String, String, ActivitySignUpBinding
    @return - Boolean
    @author - Tae hyun Park
    @since - 2022-07-12 | 2022-07-13
     **/
    @SuppressLint("ResourceAsColor")
    suspend fun startRequestSignUpValidation(
        nickName:String,
        phoneNumber: String,
        email: String,
        binding: ActivitySignUpBinding
    ): Boolean{
        var service = RetrofitManager.retrofit.create(RetrofitService::class.java) // 레트로핏 서비스 객체
        val resNickname = service.requestValidationNickName(nickName) // 닉네임 유효성 요청의 응답 response 객체
        val resPhoneNum = service.requestValidationPhoneNum(phoneNumber)  // 전화번호 유효성 요청의 응답 response 객체
        val resEmail = service.requestValidationEmail(email) // 이메일 유효성 요청의 응답 response 객체

        if(binding.etName.text.toString().isBlank()){ // 이름 필드 비워져 있는지 확인
            binding.tvNameError.setText(R.string.sign_up_none_name)
            binding.tvNameError.visibility = View.VISIBLE
        }else
            binding.tvNameError.visibility = View.GONE

        nickNameValidation(resNickname, binding) // 닉네임 검증 결과에 대한 처리

        phoneNumberValidation(resPhoneNum, binding) // 전화번호 검증 결과에 대한 처리

        emailValidation(resEmail, binding) // 이메일 검증 결과에 대한 처리

        // 세 가지 검증 요청의 성공과 이름 필드까지 입력했다면 true, 하나라도 올바르지 않다면 false 를 리턴
        return binding.etName.text.toString().isNotBlank() && resNickname.code() == 200 && resPhoneNum.code() == 200 && resEmail.code() == 200
    }

    /**
    @description - 닉네임 검증 응답 Response 를 통해 값을 구분하고 에러 처리하는 함수
    @param - Response<Unit>, ActivitySignUpBinding
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-13
     **/
    private fun nickNameValidation(resNickname: Response<Unit>, binding: ActivitySignUpBinding){
        if (resNickname.code() == 200){
            Log.d(TAG, "닉네임 검증 성공")
            binding.tvNicknameError.visibility = View.GONE
        }else{ // 올바르지 않은 응답 코드가 온 경우
            var errorString = resNickname.errorBody()?.string()!!
            Log.d(TAG, "닉네임 검증 실패 : "+errorString)
            if(binding.etNickname.text.toString().isNotBlank()){ // 닉네임을 입력했는데
                if(errorString == "{\"message\":\"이미 사용 중인 닉네임입니다.\"}"){
                    binding.tvNicknameError.setText(R.string.sign_up_error_used_nickname_text) // 중복 에러인 경우
                    binding.tvNicknameError.visibility = View.VISIBLE
                }else if(errorString == "{\"message\":\"2~10자의 영문, 한글, 숫자만 입력해주시기 바랍니다.\"}"){
                    binding.tvNicknameError.setText(R.string.sign_up_error_invalid_nickname_text) // 형식 에러인 경우
                    binding.tvNicknameError.visibility = View.VISIBLE
                }
                // 이외의 케이스에 대해 예외를 만들거나, 추가적인 처리 필요
            }else{ // 닉네임 필드가 비워져 있다면
                binding.tvNicknameError.setText(R.string.sign_up_none_nickname_text)
                binding.tvNicknameError.visibility = View.VISIBLE
            }
        }
    }

    /**
    @description - 전화번호 검증 응답 Response 를 통해 값을 구분하고 에러 처리하는 함수
    @param - Response<Unit>, ActivitySignUpBinding
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-13
     **/
    private fun phoneNumberValidation(resPhoneNum: Response<Unit>, binding: ActivitySignUpBinding){
        if (resPhoneNum.code() == 200){
            Log.d(TAG, "전화번호 검증 성공")
            binding.tvPhoneNumberError.visibility = View.GONE
        }else{ // 올바르지 않은 응답 코드가 온 경우
            if (binding.etPhoneNumber.text.toString().isNotBlank()){ // 전화번호를 입력했는데
                var errorString = resPhoneNum.errorBody()?.string()!!
                Log.d(TAG, "전화번호 검증 실패 : "+errorString)
                if(errorString == "{\"message\":\"올바른 전화번호 형식이 아닙니다.\"}"){
                    binding.tvPhoneNumberError.setText(R.string.sign_up_error_invalid_phone_number_text)    // 형식 에러인 경우
                    binding.tvPhoneNumberError.visibility = View.VISIBLE
                }else{
                    binding.tvPhoneNumberError.setText(R.string.sign_up_error_used_phone_number_text)       // 중복 에러인 경우
                    binding.tvPhoneNumberError.visibility = View.VISIBLE
                }
                // 이외의 케이스에 대해 예외를 만들거나, 추가적인 처리 필요
            }else{ // 전화번호 필드가 비워져 있다면
                binding.tvPhoneNumberError.setText(R.string.sign_up_none_phone_number_text)
                binding.tvPhoneNumberError.visibility = View.VISIBLE
            }
        }
    }

    /**
    @description - 이메일 검증 응답 Response 를 통해 값을 구분하고 에러 처리하는 함수
    @param - Response<Unit>, ActivitySignUpBinding
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-13
     **/
    private fun emailValidation(resEmail: Response<Unit>, binding: ActivitySignUpBinding){
        if (resEmail.code() == 200){
            Log.d(TAG, "이메일 검증 성공")
            binding.tvEmailError.visibility = View.GONE
        }else{
            if (binding.etEmail.text.toString().isNotBlank()){ // 이메일을 입력했는데
                var errorString = resEmail.errorBody()?.string()!!
                Log.d(TAG, "이메일 검증 실패 : "+errorString)     // 중복과 형식 2가지 케이스
                if(errorString == "{\"message\":\"올바른 이메일 형식이 아닙니다.\"}"){
                    binding.tvEmailError.setText(R.string.sign_up_error_invalid_email_text)    // 형식 에러인 경우
                    binding.tvEmailError.visibility = View.VISIBLE
                }else{
                    binding.tvEmailError.setText(R.string.sign_up_error_used_email_text)       // 중복 에러인 경우
                    binding.tvEmailError.visibility = View.VISIBLE
                }
                // 이외의 케이스에 대해 예외를 만들거나, 추가적인 처리 필요
            }else{ // 이메일 필드가 비워져 있다면
                binding.tvEmailError.setText(R.string.sign_up_none_email_text)
                binding.tvEmailError.visibility = View.VISIBLE
            }
        }
    }
}