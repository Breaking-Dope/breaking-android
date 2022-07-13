package com.dope.breaking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dope.breaking.databinding.ActivitySignUpBinding
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.utils.Util.getRealPathFromURI
import com.dope.breaking.utils.Util.RegularExpressionNickname
import com.dope.breaking.utils.Util.getBitmapWithGlide
import com.dope.breaking.utils.Util.setImageWithGlide
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class SignUpActivity : AppCompatActivity() {
    private val TAG = "SignUpActivity.kt" // Log Tag

    private var mbinding: ActivitySignUpBinding? = null // 전역 변수로 바인딩 객체 선언

    private val binding get() = mbinding!!  // 매번 null 체크할 필요 없이 바인딩 변수 재 선언

    private var imagePath: String? = "" // 이미지 경로 전역변수

    private lateinit var profileImgBitmap: Bitmap // 프로필 이미지 비트맵 전역변수

    private lateinit var galleryActivityResult: ActivityResultLauncher<Intent> // 갤러리에서 이미지를 가져왔을 때의 처리를 위한 activityResult

    private var isRoleButtonSelected: Boolean = true // 회원 유형 어떤 버튼이 눌렸는지로, true-> 일반인, false-> 언론사

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val handler = object : Handler(Looper.getMainLooper()){ // 메인 스레드에서 비트맵 변수 받아와서 할당
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                profileImgBitmap = msg.data.getParcelable("Bitmap")!! // 번들에서 비트맵 객체를 받아온다.
            }
        }

        /*
        deprecated된 OnActivityResult를 대신하는 콜백 함수로, 갤러리에서 이미지를 선택하면 호출됨.
        resultCode와 data를 가지고 있음. requestCode는 쓰이지 않음.
         */
        galleryActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            var uri = it?.data?.data // 이미지 URI

            if(it.resultCode == RESULT_OK){ // 갤러리에서 이미지를 정상적으로 선택했다면
                imagePath = uri?.let { it_let -> getRealPathFromURI(it_let, contentResolver) } // 이미지 Uri 경로 반환
                setImageWithGlide(applicationContext, uri, binding, 700,700) // Glide 라이브러리를 사용하여 회원가입 프로필 이미지 뷰에 보여주기
                getBitmapWithGlide(applicationContext, uri, handler)// Glide 라이브러리를 사용하여 파일을 비트맵으로 가져와서 저장
            }
        }

        // 닉네임 필드 정규표현식 설정
        binding.etNickname.filters = RegularExpressionNickname(
            "^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\\\u318D\\\\u119E\\\\u11A2\\\\u2022\\\\u2025a\\\\u00B7\\\\uFE55]+\$",
            10,
            applicationContext)

        // 회원가입 화면의 버튼 이벤트 함수
        clickSignUpButtons()

    }

    /**
    @description - Intent 를 이용하여 갤러리를 열고, 선택한 이미지를 처리하기 위해 galleryActivityResult 실행
    @param - None
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-08
     **/
    private fun selectGalleryIntent(){
        // 읽기, 쓰기 권한
        var writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        var readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (writePermission == PackageManager.PERMISSION_DENIED || readPermission == PackageManager.PERMISSION_DENIED) {
            // 권한 없다면 권한 요청
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            // 권한 있으면 Intent 를 통해 갤러리 Open 요청
            var intent = Intent(Intent.ACTION_PICK)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"

            galleryActivityResult.launch(intent)
        }
    }

    /**
    @description - 회원가입 화면의 모든 버튼 이벤트 처리를 모아놓은 함수
    @param - None
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-13
     **/
    private fun clickSignUpButtons(){
        // 프로필 이미지 버튼 클릭 시
        binding.imgBtnProfileImage.setOnClickListener(View.OnClickListener {
            selectGalleryIntent()
        })

        // 프로필 추가 텍스트뷰 클릭 시
        binding.tvAddProfile.setOnClickListener(View.OnClickListener {
            selectGalleryIntent()
        })

        // 회원 유형으로 일반인을 클릭한다면
        binding.btnUserTypeDefault.setOnClickListener(View.OnClickListener {
            isRoleButtonSelected = true
            binding.btnUserTypeOther.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 언론인 선택 색상 비활성화
            binding.btnUserTypeDefault.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 일반인 선택 색상 활성화
        })

        // 회원 유형으로 언론인을 클릭한다면
        binding.btnUserTypeOther.setOnClickListener(View.OnClickListener {
            isRoleButtonSelected = false
            binding.btnUserTypeOther.setBackgroundResource(R.drawable.sign_up_user_type_selected)    // 언론인 선택 색상 활성화
            binding.btnUserTypeDefault.setBackgroundResource(R.drawable.sign_up_user_type_unselected)// 일반인 선택 색상 비활성화
        })

        // 최종적으로 회원가입 버튼을 클릭한다면
        binding.btnUserRegister.setOnClickListener(View.OnClickListener {
            // 회원가입의 입력 필드 값 모두 가져오기
            var realName = binding.etName.text.toString()     // 이름
            var nickName = binding.etNickname.text.toString() // 닉네임
            var phoneNumber = binding.etPhoneNumber.text.toString() // 전화번호
            var email = binding.etEmail.text.toString()             // 이메일
            var stateMessage = binding.etStateMessage.text.toString() // 상태 메시지
            var role = isRoleButtonSelected              // 회원 유형 (Default 값은 true)

            Log.d(TAG,"\n이름 : "+realName
                    +"\n닉네임 : "+nickName
                    +"\n전화번호 : " +phoneNumber+
                    "\n이메일 : "+email+
                    "\n상태 메시지 : "+stateMessage+
                    "\n회원 유형 : "+role)

            // 닉네임, 전화번호, 이메일의 유효성 검증 요청
            CoroutineScope(Dispatchers.Main).launch {
                val validation = Validation()
                val validationResult = validation.startRequestSignUpValidation(nickName, phoneNumber, email, binding)
                Log.d(TAG, "3가지 검증 요청 결과 : "+validationResult.toString())
            }
            // 이름, 닉네임, 전화번호, 이메일 유효성 검증이 성공하고 회원가입 요청 함수에 전달..프로필 이미지 선택 안하면 기본 이미지 비트맵 전달하면 됨.

        })
    }

}