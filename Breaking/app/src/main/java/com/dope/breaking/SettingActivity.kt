package com.dope.breaking

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dope.breaking.databinding.ActivitySettingBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.signup.Account
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        binding.tbSetting.setNavigationIcon(R.drawable.ic_baseline_arrow_back_black_24)
        binding.tbSetting.setNavigationOnClickListener {
            finish()
        }
        binding.tvLogOut.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val token = ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(this@SettingActivity).getAccessTokenFromLocal()
                try{
                    val result = Account().startRequestLogOut(token)
                    if(result){
                        ResponseExistLogin.baseUserInfo = null
                        moveToLoginPage()
                    }
                }catch (e: ResponseErrorException){

                }
            }
        }
        setContentView(binding.root)
    }

    private fun moveToLoginPage(){
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}