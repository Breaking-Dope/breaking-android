package com.dope.breaking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dope.breaking.databinding.ActivitySignUpBinding
import com.dope.breaking.exception.MissingJwtTokenException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.request.RequestSignUp
import com.dope.breaking.model.response.ResponseLogin
import com.dope.breaking.signup.Account
import com.dope.breaking.signup.ValidationSignUp
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.Utils.regularExpressionNickname
import com.dope.breaking.util.Utils.getBitmapWithGlide
import com.dope.breaking.util.Utils.getFileNameFromURI
import com.dope.breaking.util.Utils.setImageWithGlide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.ValueUtil

class SignUpActivity : AppCompatActivity() {
    private var mbinding: ActivitySignUpBinding? = null // 전역 변수로 바인딩 객체 선언

    private val binding get() = mbinding!!  // 매번 null 체크할 필요 없이 바인딩 변수 재 선언

    private var isRoleButtonSelected: Boolean = true // 회원 유형 어떤 버튼이 눌렸는지로, true-> 일반인, false-> 언론사

    private var profileImgBitmap: Bitmap? = null // 프로필 이미지 비트맵 전역변수

    private var filename: String? = null // 프로필 이미지 파일명

    private var validationResult: Boolean = false // 회원가입 검증 결과값

    private lateinit var galleryActivityResult: ActivityResultLauncher<Intent> // 갤러리에서 이미지를 가져왔을 때의 처리를 위한 activityResult

    private lateinit var responseBody: ResponseLogin // 로그인 시 생성되는 응답 객체

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 페이지로부터 받아온 response 객체 할당
        val intentFromSignIn = intent
        if (intentFromSignIn != null) {
            val data = intentFromSignIn.getSerializableExtra("responseBody")
            if (data is ResponseLogin)
                responseBody = data
        }

        val handler = object : Handler(Looper.getMainLooper()) { // 메인 스레드에서 비트맵 변수 받아와서 할당
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                profileImgBitmap = msg.data.getParcelable("Bitmap")!! // 번들에서 비트맵 객체를 받아온다.
            }
        }

        /*
        deprecated된 OnActivityResult를 대신하는 콜백 함수로, 갤러리에서 이미지를 선택하면 호출됨.
        resultCode와 data를 가지고 있음. requestCode는 쓰이지 않음.
         */
        galleryActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val uri = it.data?.data // 이미지 URI

                if (it.resultCode == RESULT_OK) { // 갤러리에서 이미지를 정상적으로 선택했다면
                    filename = getFileNameFromURI(uri!!, contentResolver)
                    setImageWithGlide(
                        applicationContext,
                        uri,
                        binding,
                        0,
                        0
                    ) // Glide 라이브러리를 사용하여 회원가입 프로필 이미지 뷰에 보여주기
                    getBitmapWithGlide(
                        applicationContext,
                        uri,
                        handler
                    )// Glide 라이브러리를 사용하여 파일을 비트맵으로 가져와서 저장
                }
            }

        binding.etNickname.filters = regularExpressionNickname( // 닉네임 필드 정규표현식 설정
            "^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\\\u318D\\\\u119E\\\\u11A2\\\\u2022\\\\u2025a\\\\u00B7\\\\uFE55]+\$",
            10,
            applicationContext
        )

        binding.etEmail.setText(responseBody.userEmail) // 회원가입 이메일 기본값으로 카카오에서 받아온 이메일

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
    private fun selectGalleryIntent() {
        // 읽기, 쓰기 권한
        val writePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (writePermission == PackageManager.PERMISSION_DENIED || readPermission == PackageManager.PERMISSION_DENIED) {
            // 권한 없다면 권한 요청
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        } else {
            // 권한 있으면 Intent 를 통해 갤러리 Open 요청
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")

            galleryActivityResult.launch(intent)
        }
    }

    /**
     * 모듈들을 불러서 회원가입 요청을 하는 프로세스 함수
     * @param inputData(RequestSignUp): 필드 데이터 객체
     * @param imageData(Bitmap): bitmap 형태의 프로필 이미지 데이터
     * @param imageName(String): 이미지 데이터의 파일 이름
     * @return None
     * @author Seunggun Sin
     * @since 2022-07-11
     */
    private suspend fun processSignUp(
        inputData: RequestSignUp,
        imageData: Bitmap?,
        imageName: String
    ) {
        try {
            val account = Account() // 커스텀 회원가입 객체 생성
            // 회원가입 요청 함수 signature. 아래 형식대로 회원가입 요청 함수 호출.
            // 현재 input 은 테스트용이므로 실제 input 값으로 대체 바람
            val responseHeaders = account.startRequestSignUp(
                inputData = inputData,
                imageData = imageData,
                imageName = imageName
            )

            val tokenUtil = JwtTokenUtil(applicationContext) // Jwt Util 객체 생성
            val accessToken =
                tokenUtil.getAccessTokenFromResponse(responseHeaders) // 응답으로부터 Jwt 엑세스 토큰 값 추출
            val refreshToken =
                tokenUtil.getRefreshTokenFromResponse(responseHeaders) // 응답으로부터 Jwt refresh 토큰 값 추출

            if (accessToken != null && refreshToken != null) { // 토큰이 존재하면
                tokenUtil.setAccessToken(accessToken) // SharedPreferences 를 이용하여 Jwt 토큰을 로컬에 저장
                tokenUtil.setRefreshToken(refreshToken) // SharedPreferences 를 이용하여 refresh 토큰을 로컬에 저장

                //회원가입 응답으로 받아온 토큰 값을 토대로 유저의 기본 정보 가져오는 요청
                val userInfo =
                    tokenUtil.validateJwtToken(ValueUtil.JWT_REQUEST_PREFIX + accessToken)
                moveToMainPage(userInfo) // 가져온 유저 정보 값을 가지고 메인 페이지로 이동하기
            } else {
                // 존재하지 않는 Jwt 토큰 케이스에 대한 예외 던지기
                throw MissingJwtTokenException("Jwt 토큰이 존재하지 않습니다.")
            }
        } catch (e: ResponseErrorException) {
            e.printStackTrace()
            DialogUtil().SingleDialog(
                this,
                "회원가입 요청에 문제가 발생하였습니다.",
                "확인"
            ).show()
            // 응답 에러에 대한 예외 처리하기
        } catch (e: MissingJwtTokenException) {
            // 토큰 값이 존재하지 않을 때에 대한 예외 처리하기
            DialogUtil().SingleDialog(
                this,
                "회원가입 정보를 불러오는데 실패하였습니다.",
                "확인"
            ).show()
        }
    }

    /**
    @description - 회원가입 화면의 모든 버튼 이벤트 처리를 모아놓은 함수
    @param - None
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-13
     **/
    private fun clickSignUpButtons() {
        // 프로필 이미지 버튼 클릭 시
        binding.imgBtnProfileImage.setOnClickListener {
            selectGalleryIntent()
        }

        // 프로필 추가 텍스트뷰 클릭 시
        binding.tvAddProfile.setOnClickListener {
            selectGalleryIntent()
        }

        // 회원 유형으로 일반인을 클릭한다면
        binding.btnUserTypeDefault.setOnClickListener {
            isRoleButtonSelected = true
            binding.btnUserTypeOther.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 언론인 선택 색상 비활성화
            binding.btnUserTypeDefault.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 일반인 선택 색상 활성화
        }

        // 회원 유형으로 언론인을 클릭한다면
        binding.btnUserTypeOther.setOnClickListener {
            isRoleButtonSelected = false
            binding.btnUserTypeOther.setBackgroundResource(R.drawable.sign_up_user_type_selected)    // 언론인 선택 색상 활성화
            binding.btnUserTypeDefault.setBackgroundResource(R.drawable.sign_up_user_type_unselected)// 일반인 선택 색상 비활성화
        }

        // 최종적으로 회원가입 버튼을 클릭한다면
        binding.btnUserRegister.setOnClickListener {
            // 회원가입의 입력 필드 값 모두 가져오기
            val realName = binding.etName.text.toString()     // 이름
            val nickName = binding.etNickname.text.toString() // 닉네임
            val phoneNumber = binding.etPhoneNumber.text.toString() // 전화번호
            val email = binding.etEmail.text.toString()             // 이메일
            val stateMessage = binding.etStateMessage.text.toString() // 상태 메시지
            val role = isRoleButtonSelected              // 회원 유형 (Default 값은 true)

            // 닉네임, 전화번호, 이메일의 유효성 검증 요청
            val progressDialog = DialogUtil().ProgressDialog(this)
            progressDialog.showDialog()

            CoroutineScope(Dispatchers.Main).launch {
                val validationSignUp = ValidationSignUp()
                validationResult =
                    validationSignUp.startRequestSignUpValidation(
                        nickName,
                        phoneNumber,
                        email,
                        binding,
                        ""
                    )

                // 유효성 검증에 성공했다면 최종 회원가입 요청을 보냄.

                if (validationResult) {
                    val inputData = RequestSignUp(
                        responseBody.userName,
                        nickName,
                        phoneNumber,
                        email,
                        realName,
                        stateMessage,
                        role
                    )
                    // 회원가입 요청 시작, 회원가입 버튼 클릭하고 검증 완료 후 input 데이터와 함께 호출
                    processSignUp(
                        inputData = inputData,
                        imageData = profileImgBitmap,
                        imageName = filename ?: "default.png"
                    )

                    if (progressDialog.isShowing()) { // 로딩 progress 종료
                        progressDialog.dismissDialog()
                    }
                } else {
                    if (progressDialog.isShowing()) { // 로딩 progress 종료
                        progressDialog.dismissDialog()
                    }
                }
            }
        }
    }

    /**
     * 유저 정보를 갖고 메인 피드 페이지로 이동하는 메소드 (임시로 MainActivity 로 설정)
     * @param userInfo(ResponseJwtUserInfo): Jwt 토큰 확인을 통해 받은 Response DTO 객체
     * @return None
     * @author Seunggun Sin
     * @since 2022-07-11
     */
    private fun moveToMainPage(userInfo: ResponseExistLogin) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("userInfo", userInfo)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

}