package com.dope.breaking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.dope.breaking.databinding.ActivitySignUpBinding
import com.dope.breaking.model.request.RequestUpdateUser
import com.dope.breaking.model.response.DetailUser
import com.dope.breaking.signup.Account
import com.dope.breaking.signup.ValidationSignUp
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.Utils
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private var isRoleButtonSelected: Boolean = true // 회원 유형 버튼 flag 값
    private lateinit var galleryActivityResult: ActivityResultLauncher<Intent> // 갤러리에서 이미지를 가져왔을 때의 처리를 위한 activityResult
    private var filename: String = "" // 프로필 이미지 파일명
    private var profileImgBitmap: Bitmap? = null // 프로필 이미지 비트맵 전역변수
    private var isDefaultImage = false // 기본 이미지를 사용하냐 안하냐 구분
    private val handler = object : Handler(Looper.getMainLooper()) { // 메인 스레드에서 비트맵 변수 받아와서 할당
        override fun handleMessage(msg: Message) {
            profileImgBitmap = msg.data.getParcelable("Bitmap")!! // 번들에서 비트맵 객체를 받아온다.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        initView() // 초기 화면 세팅

        // 프로필 이미지 버튼 클릭 시
        binding.imgBtnProfileImage.setOnClickListener(View.OnClickListener {
            selectGalleryIntent()
        })

        // 프로필 추가 텍스트뷰 클릭 시
        binding.tvAddProfile.setOnClickListener(View.OnClickListener {
            selectGalleryIntent()
        })

        // 회원 유형 - 일반인 클릭 시
        binding.btnUserTypeDefault.setOnClickListener {
            isRoleButtonSelected = true
            binding.btnUserTypeOther.setBackgroundResource(R.drawable.sign_up_user_type_unselected)
            binding.btnUserTypeDefault.setBackgroundResource(R.drawable.sign_up_user_type_selected)
        }
        // 회원 유형 - 언론인 클릭 시
        binding.btnUserTypeOther.setOnClickListener {
            isRoleButtonSelected = false
            binding.btnUserTypeOther.setBackgroundResource(R.drawable.sign_up_user_type_selected)
            binding.btnUserTypeDefault.setBackgroundResource(R.drawable.sign_up_user_type_unselected)
        }

        if (intent != null) {
            val data = intent.getSerializableExtra("detail") as DetailUser
            fillDataInView(data) // 각 View 에 가져온 데이터 채우기

            galleryActivityResult =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == RESULT_OK) { // 갤러리에서 이미지를 정상적으로 선택했다면
                        val uri = it.data?.data!! // 이미지 URI

                        filename = Utils.getFileNameFromURI(uri, contentResolver)!!
                        Utils.setImageWithGlide(
                            applicationContext,
                            uri,
                            binding,
                            0,
                            0
                        ) // Glide 라이브러리를 사용하여 회원가입 프로필 이미지 뷰에 보여주기
                        Utils.getBitmapWithGlide(
                            applicationContext,
                            uri,
                            handler
                        )// Glide 라이브러리를 사용하여 파일을 비트맵으로 가져와서 저장
                    }
                }

            // 프로필 수정 완료 버튼 클릭 시
            binding.btnUserRegister.setOnClickListener {
                val realName = binding.etName.text.toString()
                val email = binding.etEmail.text.toString()
                val nickname = binding.etNickname.text.toString()
                val phoneNumber = binding.etPhoneNumber.text.toString()
                val statusMsg = binding.etStateMessage.text.toString()
                val role = if (isRoleButtonSelected) "USER" else "PRESS"

                val token = JwtTokenUtil(this).getAccessTokenFromLocal() // 로컬에서 토큰 가져오기

                val progressDialog = DialogUtil().ProgressDialog(this)
                progressDialog.showDialog() // 로딩 dialog 시작

                CoroutineScope(Dispatchers.Main).launch {
                    val validationSignUp = ValidationSignUp()
                    val result =
                        validationSignUp.startRequestSignUpValidation(
                            nickname,
                            phoneNumber,
                            email,
                            binding,
                            ValueUtil.JWT_REQUEST_PREFIX + token
                        ) // 닉네임, 전화번호, 이메일 검증하기

                    if (result) { // 검증에 성공했다면
                        val inputData = RequestUpdateUser(
                            nickname,
                            phoneNumber,
                            email,
                            realName,
                            role,
                            statusMsg,
                            profileImgBitmap != null || isDefaultImage
                        ) // 필드 객체

                        val account = Account()
                        val res = account.startRequestUpdateProfile(
                            inputData,
                            profileImgBitmap,
                            filename,
                            ValueUtil.JWT_REQUEST_PREFIX + token
                        ) // 프로필 변경 요청

                        if (progressDialog.isShowing())
                            progressDialog.dismissDialog() // 로딩 종료

                        // 변경 요청 성공 시
                        if (res)
                            finish() // 화면 종료
                        else { // 요청 실패 시
                            DialogUtil().SingleDialog(
                                this@EditProfileActivity,
                                "회원 정보 수정에 실패하였습니다!",
                                "확인"
                            ).show() // 에러 메세지 보여주기
                        }
                    } else { // 검증 실패 시
                        if (progressDialog.isShowing())
                            progressDialog.dismissDialog() // 로딩 종료
                    }
                }

            }
        }
        setContentView(binding.root)
    }

    /**
     * 초기 View 에 데이터 채워넣기
     * @param data(DetailUser): 기존 유저 데이터
     * @author Seunggun Sin
     * @since 2022-07-25 | 2022-08-29
     */
    private fun fillDataInView(data: DetailUser) {
        binding.etName.setText(data.realName)
        binding.etEmail.setText(data.email)
        binding.etNickname.setText(data.nickname)
        binding.etPhoneNumber.setText(data.phoneNumber)
        binding.etStateMessage.setText(data.statusMsg)
        if (data.profileImgURL != null) {
            Glide.with(this)
                .load(ValueUtil.IMAGE_BASE_URL + data.profileImgURL)
                .placeholder(R.drawable.ic_default_profile_image)
                .circleCrop()
                .into(binding.imgBtnProfileImage)
        } else {
            Glide.with(this)
                .load(R.drawable.ic_default_profile_image)
                .circleCrop()
                .into(binding.imgBtnProfileImage)
        }
    }

    /**
     * 초기 변경되야 하는 View 세팅
     * @author Seunggun Sin
     * @since 2022-07-25
     */
    private fun initView() {
        binding.tvAddProfile.text = "프로필 수정"
        binding.btnUserRegister.text = "완료"
        binding.imgBtnBack.visibility = View.VISIBLE
        binding.imgBtnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * 로컬 파일 읽기 쓰기 권한 여부 체크 및 Intent 를 통한 갤러리 열기 (Tae hyun Park 이 작성한 메소드 참조)
     * @author Seunggun Sin (Refer to Tae hyun)
     * @since 2022-07-25
     */
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
            DialogUtil().MultipleDialog(
                this,
                "       프로필 이미지 변경하기       ",
                "기본 이미지 사용",
                "갤러리에서 가져오기",
                {
                    Glide.with(this)
                        .load(ValueUtil.getDefaultProfile(this))
                        .circleCrop()
                        .into(binding.imgBtnProfileImage) // 기본 이미지로 보여주기
                    profileImgBitmap = null
                    filename = ""
                    isDefaultImage = true // 기본 이미지 사용
                },
                {
                    // 갤러리 intent 호출
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                    galleryActivityResult.launch(intent)
                }, true
            ).show()

        }
    }

}