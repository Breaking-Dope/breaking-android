package com.dope.breaking.board

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color.RED
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dope.breaking.R
import com.dope.breaking.databinding.ActivityPostBinding
import com.dope.breaking.exception.ErrorFileSelectedException
import com.dope.breaking.exception.FailedGetLocationException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.map.KakaoMapActivity
import com.dope.breaking.model.LocationList
import com.dope.breaking.model.request.RequestPostData
import com.dope.breaking.model.PostLocation
import com.dope.breaking.post.ValidationPost
import com.dope.breaking.post.PostManager
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.Utils
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList


class PostActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {

    private val TAG = "PostActivity.kt" // Tag Log

    private var mbinding: ActivityPostBinding? = null

    private val binding get() = mbinding!!

    private lateinit var galleryActivityResult: ActivityResultLauncher<Intent> // 갤러리에서 이미지 혹은 영상을 가져왔을 때 처리를 위한 activityResult

    private lateinit var locationActivityResult: ActivityResultLauncher<Intent> // 카카오 맵으로부터 위치를 선택하고 돌아온다면 그 후 처리를 위한 activityResult

    private lateinit var adapter: MultiImageAdapter // 리사이클러뷰에 적용시킬 어댑터

    private lateinit var uriList: ArrayList<Uri> // 이미지,영상에 대한 Uri 리스트

    private lateinit var fileNameList: ArrayList<String> // 제보 이미지 파일명 리스트

    private lateinit var postBitmapList: ArrayList<Bitmap> // 제보 게시글의 비트맵 리스트

    private lateinit var locationData: LocationList // 받아올 제보 위치 DTO

    private lateinit var hashTagList: ArrayList<String> // 해시 태그 리스트

    private lateinit var eventTime: String // 제보 발생 시간

    private var isPostTypeSelected: String =
        "charged" // 제보 방식으로, 단독, 유료, 무료 제보로 나뉘고 기본값은 유료 제보로 설정.

    private var isAnonymousSelected: Boolean = false // 익명 여부로, true-> 익명, false-> 공개이고 기본값은 공개로 설정.

    private var postPriceString: String = "" // 제보 가격 콤마 표시를 위한 저장 변수

    private var decimalFormat = DecimalFormat("#,###") // 제보 가격 콤마 표시를 위한 포맷

    /* 데이터 피커와 타임 피커에 필요한 전역 변수들로, 년, 월, 일, 시, 분, 초를 포함한다. */
    private var day = 0
    private var month = 0
    private var year = 0
    private var hour = 0
    private var min = 0
    private var second = 0

    /* 사용자가 시간을 선택할 때 저장해둘 전역 변수들 */
    private var savedDay = 0
    private var savedMonth = 0
    private var savedYear = 0
    private var savedHour = 0
    private var savedMin = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val handler = object : Handler(Looper.getMainLooper()) { // 메인 스레드에서 비트맵 변수 받아와서 할당
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                postBitmapList.add(msg.data.getParcelable("Bitmap")!!)
            }
        }

        val textWatcher = object : TextWatcher { // 제보 가격 입력 필드 실시간 감지 콜백 함수
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!TextUtils.isEmpty(s.toString()) && s.toString() != postPriceString) { // 가격 입력 필드가 비워져 있지 않고, 값 변동이 없을 때만 작동하도록 (무한 루프 방지)
                    postPriceString = decimalFormat.format((s.toString().replace(",", "")).toInt());
                    binding.etPostPrice.setText(postPriceString); // ,이용하여 가격 표시
                    binding.etPostPrice.setSelection(postPriceString.length); // 커서 위치 변경
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        uriList = ArrayList<Uri>() // Uri 리스트 초기화
        postBitmapList = ArrayList<Bitmap>() // 비트맵 리스트 초기화
        fileNameList = ArrayList<String>()  // 미디어 파일명 리스트 초기화
        hashTagList = ArrayList<String>() // 해시 태그 리스트 초기화
        binding.tvEventTimeClicked.text = Utils.getCurrentTime(false) // 현재 시간 출력
        eventTime = Utils.getCurrentTime(true) // 발생 시간 현재 시간으로 기본 할당
        binding.tvLocationShow.text = "경기도 성남시" // 현재 위치 임시로 할당
        binding.etPostPrice.addTextChangedListener(textWatcher) // 제보 가격 콜백 함수 등록

        settingPostToolBar()  // 툴 바 설정
        allowScrollEditText() // EditText 스크롤 터치 이벤트 허용
        pickDate() // 제보 이벤트 발생 시간 선택을 위한 데이트, 타임 피커
        clickPostPageButtons() // 제보하기 페이지들의 버튼 이벤트 함수
        setCurrentLocation() // 현재 위치 설정 함수

        /*
        deprecated된 OnActivityResult를 대신하는 콜백 함수로, 갤러리에서 이미지나 영상을 선택하면 호출됨.
        resultCode와 data를 가지고 있음. requestCode는 쓰이지 않음.
         */
        galleryActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {   // 갤러리에서 이미지를 정상적으로 선택했다면
                    try {
                        if (it.data?.clipData == null) { // 여러 장 선택을 지원하지 않는 기기에서 이미지&영상을 하나만 선택한 경우 (연식 오래된 구 휴대폰들)
                            var uri = it.data?.data
                            if (uriList.size < 10) {
                                uriList.add(uri!!)
                                adapter = MultiImageAdapter(
                                    uriList,
                                    fileNameList,
                                    postBitmapList,
                                    applicationContext,
                                    binding
                                )
                                binding.viewRecyclerImage.adapter = adapter
                                binding.viewRecyclerImage.layoutManager = LinearLayoutManager(
                                    this,
                                    LinearLayoutManager.HORIZONTAL,
                                    true
                                ) // 수평 스크롤 적용
                            } else
                                Toast.makeText(
                                    applicationContext,
                                    "제보 미디어 파일은 총 20장까지 가능합니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            // 해당 버전에서 파일명 처리는 추후에 할 예정

                        } else { // 여러 장 선택을 지원하는 기기에서 이미지&영상을 한 장 혹은 여러 장 선택한 경우
                            var clipData = it.data?.clipData
                            if ((clipData!!.itemCount + uriList.size) <= 20) { // 내가 올려놓은 사진들의 개수와 갤러리에서 추가적으로 선택한 사진 개수의 합이 10장을 초과하지 않아야 저장 O
                                changeUploadImageColors(
                                    clipData!!.itemCount,
                                    uriList.size
                                ) // 최대 개수면 사진 카운트 텍스트 색상 변경
                                for (i in 0 until clipData.itemCount) {   // 선택한 미디어 파일의 개수만큼 반복문
                                    var uri =
                                        clipData.getItemAt(i).uri // 해당 인덱스의 선택한 이미지의 uri를 가져오기
                                    fileNameList.add(
                                        Utils.getFileNameFromURI(
                                            uri!!,
                                            contentResolver
                                        )!!
                                    )
                                    uriList.add(uri)
                                    Utils.getBitmapWithGlide(
                                        applicationContext,
                                        uri,
                                        handler
                                    ) // 비트맵 리스트에 하나씩 추가
                                }
                                adapter = MultiImageAdapter(
                                    uriList,
                                    fileNameList,
                                    postBitmapList,
                                    applicationContext,
                                    binding
                                )
                                binding.viewRecyclerImage.adapter = adapter
                                binding.viewRecyclerImage.layoutManager = LinearLayoutManager(
                                    this,
                                    LinearLayoutManager.HORIZONTAL,
                                    true
                                ) // 수평 스크롤 적용
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "제보 미디어 파일은 총 20장까지 가능합니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: ErrorFileSelectedException) {
                        DialogUtil().SingleDialog( // 미디어 선택에 문제가 있는 경우
                            this,
                            "미디어 파일 선택 중 에러가 발생하였습니다.",
                            "확인"
                        ) {}.show()
                    }
                }
            }

        // 카카오 지도에서 제보 위치를 선택하고 제보 페이지로 다시 돌아오면
        locationActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    locationData =
                        it.data?.getSerializableExtra("locationData") as LocationList // 선택한 장소 위치 정보 받아와 할당
                    binding.tvLocationShow.text = locationData.address // 선택한 위치 정보 표시
                }
            }
    }

    /**
     * @description - 제보하기 페이지 클릭 관련 콜백 함수를 모아놓은 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-31 | 2022-08-09
     */
    private fun clickPostPageButtons() {
        // 이미지 제보 버튼이 눌렸다면 갤러리 인텐트 함수 호출
        binding.layoutImageButton.setOnClickListener {
            selectGalleryIntent()
        }

        // 이미지 제보 버튼이 눌렸다면 갤러리 인텐트 함수 호출
        binding.ibUploadPicture.setOnClickListener {
            selectGalleryIntent()
        }

        /* 아래 3개는 현재 위치 불러오는 함수 */
        binding.tvLocationShow.setOnClickListener {
            var intent = Intent(this, KakaoMapActivity::class.java)
            locationActivityResult.launch(intent)
        }

        binding.tvLocationIcon.setOnClickListener {
            var intent = Intent(this, KakaoMapActivity::class.java)
            locationActivityResult.launch(intent)
        }

        binding.tvLocationIconText.setOnClickListener {
            var intent = Intent(this, KakaoMapActivity::class.java)
            locationActivityResult.launch(intent)
        }

        // 유료 제보 버튼 누르면
        binding.btnPostTypeCharged.setOnClickListener {
            isPostTypeSelected = "charged"
            binding.btnPostTypeCharged.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 유료 제보 색상 활성화
            binding.btnPostTypeFree.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 무료 제보 색상 비활성화
            binding.btnPostTypeExclusive.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 단독 제보 색상 비활성화
            binding.etPostPrice.setText("") // 가격을 다시 초기화
            binding.etPostPrice.isEnabled = true // 사용자로부터 가격 설정 가능하게 설정
        }

        // 무료 제보 버튼 누르면
        binding.btnPostTypeFree.setOnClickListener {
            isPostTypeSelected = "free"
            binding.btnPostTypeFree.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 무료 제보 색상 활성화
            binding.btnPostTypeCharged.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 유료 제보 색상 비활성화
            binding.btnPostTypeExclusive.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 단독 제보 색상 비활성화
            binding.etPostPrice.setText("0") // 가격을 자동으로 0원으로 설정
            binding.etPostPrice.isEnabled = false // 사용자로부터 가격 설정 불가능하도록 설정
        }

        // 단독 제보 버튼 누르면
        binding.btnPostTypeExclusive.setOnClickListener {
            isPostTypeSelected = "exclusive"
            binding.btnPostTypeExclusive.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 단독 제보 색상 활성화
            binding.btnPostTypeFree.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 무료 제보 색상 비활성화
            binding.btnPostTypeCharged.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 유료 제보 선택 색상 비활성화
            binding.etPostPrice.setText("") // 가격을 다시 초기화
            binding.etPostPrice.isEnabled = true // 사용자로부터 가격 설정 가능하게 설정
        }

        // 익명 버튼 누르면
        binding.btnPostProfileTypeSecret.setOnClickListener {
            isAnonymousSelected = true
            binding.btnPostProfileTypeSecret.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 익명 버튼 색상 활성화
            binding.btnPostProfileTypePublic.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 공개 버튼 색상 비활성화
        }

        // 공개 버튼 누르면
        binding.btnPostProfileTypePublic.setOnClickListener {
            isAnonymousSelected = false
            binding.btnPostProfileTypePublic.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 공개 버튼 색상 활성화
            binding.btnPostProfileTypeSecret.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 익명 버튼 색상 비활성화
        }

        // 제보하기 버튼을 누르면
        binding.btnPostRegisterBtn.setOnClickListener {
            // 미디어 파일 선택 안하면, 기본 이미지 넣어주도록 처리 필요
            if (postBitmapList.size == 0) {
                postBitmapList.add(ValueUtil.getDefaultPost(this@PostActivity))
                fileNameList.add("default.png")
            } else {
                postBitmapList.removeAt(0)
                fileNameList.removeAt(0)
            }

            // 해시 태그 값이 있는지 확인, 태그가 없다면 기본 값으로 다시 초기화
            if (binding.etContent.text.toString().indexOf('#') !== -1) {
                hashTagList =
                    Utils.getArrayHashTag(binding.etContent.text.toString()) // 해시 태그 처리하여 태그 문자열 추출
            } else {
                hashTagList.clear() // 해시 태그 값이 없으면 리스트 재 초기화
            }

            // 필드 검증
            val validationPost = ValidationPost()
            if (validationPost.startPostValidation(binding)) { // 필드 검증이 성공적이라면
                // 요청 DTO 생성
                val requestPostData = RequestPostData(
                    binding.etTitle.text.toString(),
                    binding.etContent.text.toString(),
                    PostLocation(
                        locationData.address,
                        locationData.y,
                        locationData.x,
                        locationData.address.split(" ")[0],
                        locationData.address.split(" ")[1]
                    ),
                    binding.etPostPrice.text.toString().replace(",", "").toInt(),
                    hashTagList,
                    isPostTypeSelected,
                    isAnonymousSelected,
                    eventTime,
                    0
                )
                // 게시글 작성 요청 다이얼 로그 시작
                val progressDialog = DialogUtil().ProgressDialog(this)
                progressDialog.showDialog()

                // 게시글 작성 요청
                CoroutineScope(Dispatchers.Main).launch {
                    processPost(
                        requestPostData,
                        postBitmapList,
                        fileNameList,
                        JwtTokenUtil(applicationContext).getAccessTokenFromLocal() // 로컬에서 토큰 가져오기
                    )
                    if (progressDialog.isShowing()) { // 로딩 다이얼로그 종료
                        progressDialog.dismissDialog()
                    }
                }
            }
        }
    }

    /**
     * @description - 게시글 인풋 정보와 JWT 토큰을 받아와 게시글 작성 요청 함수를 호출하는 메소드
     * @param - inputData(RequestPostData) : 게시글 인풋 정보
     * @param - imageData(ArrayList<Bitmap>) : 미디어 비트맵 리스트
     * @param - imageName(ArrayList<String>) : 미디어 파일명 리스트
     * @param - token(String) : JWT 토큰
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-02
     */
    private suspend fun processPost(
        inputData: RequestPostData,
        imageData: ArrayList<Bitmap>,
        imageName: ArrayList<String>,
        token: String
    ) {
        val postManager = PostManager() // 커스텀 게시글 객체 생성
        try {
            val responsePostUpload = postManager.startPostUpload(
                inputData,
                imageData,
                imageName,
                token
            )
            Log.d(TAG, "요청 성공 시 받아온 postId : ${responsePostUpload.postId}")
            Toast.makeText(applicationContext, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
            intent.putExtra("isWritePost", true) // 메인 피드로 제보글 상태값 전달
            setResult(RESULT_OK, intent)
            finish() // 제보하기 페이지 종료
        } catch (e: ResponseErrorException) {
            e.printStackTrace()
            DialogUtil().SingleDialog(
                this,
                "게시글 작성 요청에 문제가 발생하였습니다.",
                "확인"
            )
        }
    }

    /**
     * @description - 제보 페이지에서 기본값으로 현재 자신의 위치를 설정하는 함수, 위도 경도와 주소를 받아오고, locationData 에 할당하는 함수
     * @param - None
     * @return - None
     * @author - Tae Hyun Park
     * @since - 2022-08-10
     */
    @SuppressLint("MissingPermission") // 권한 재확인 안하기 위한 코드로, 코드 검사에서 제외할 부분을 미리 정의하는 것이라고 보면 됨.
    private fun setCurrentLocation() {
        // 현재 위치 불러오기
        val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val userNowLocation: Location? = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        // 위도 , 경도
        val uLatitude = userNowLocation?.latitude!!
        val uLongitude = userNowLocation?.longitude!!

        // 좌표를 바탕으로 현재 위치의 주소 불러오기 (Geocoder)
        val address = Geocoder(applicationContext).getFromLocation(uLatitude, uLongitude, 10)
        try {
            if (address != null) {
                if (address.isNullOrEmpty()) // 현재 위치 불러오기에 실패했다면
                    throw FailedGetLocationException("현재 위치를 불러오는데 실패하였습니다.")
                else {
                    binding.tvLocationShow.setText(
                        (address as MutableList<Address>)[0].getAddressLine(
                            0
                        ).substring(5)
                    ) // 선택한 위치 정보 표시
                    locationData = LocationList(
                        "", // 장소 이름
                        "", // 도로명
                        address[0].getAddressLine(0).substring(5), // 전체 주소
                        uLongitude,
                        uLatitude
                    )
                }
            }
        } catch (e: FailedGetLocationException) {
            e.printStackTrace()
            DialogUtil().SingleDialog(
                applicationContext,
                "현재 위치 정보를 불러오는데 문제가 발생하였습니다.",
                "확인"
            )
        }
    }

    /**
     * @description - Intent 를 이용하여 갤러리를 열고, 선택한 이미지를 처리하기 위해 galleryActivityResult 실행
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-08 | 2022-07-27
     **/
    private fun selectGalleryIntent() {
        // 읽기, 쓰기 권한
        var writePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        var readPermission =
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
            var intent = Intent(Intent.ACTION_PICK)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/* video/*"  // 갤러리에서 이미지, 영상 둘 다 선택 가능하도록 허용
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 여러 개를 선택할 수 있도록 다중 옵션 지정
            galleryActivityResult.launch(intent)
        }
    }

    /**
     * @description - activity_post.xml에서 이미 최상위 NestedScrollView가 정의되어 있기 때문에 EditText에 스크롤 옵션을 주어도 이벤트가 막히는 현상이 발생한다.
    따라서 해당 메소드를 통해 EditText 가 터치되어있을 때 부모의 스크롤 권한을 가로채고, EditText가 아닌 바깥을 터치한다면 다시 부모 스크롤 뷰가 동작하도록
    하는 메소드.
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-25
     */
    private fun allowScrollEditText() {
        // EditText 스크롤 터지 이벤트 리스너
        binding.etContent.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (v!!.id === R.id.et_content) { // 안쪽 EditText 를 클릭했다면
                    v!!.parent.requestDisallowInterceptTouchEvent(true) // 부모 뷰의 스크롤 이벤트 비허용
                    when (event!!.action and MotionEvent.ACTION_MASK) { // 만약 바깥 이벤트가 클릭되었다면
                        MotionEvent.ACTION_UP -> v!!.parent.requestDisallowInterceptTouchEvent(false) // 부모 뷰의 스크롤 이벤트 허용
                    }
                }
                return false
            }
        })
    }

    /**
     * @description - 사진 최대 개수에 도달하면 개수 카운트 색상을 빨간색으로 바꿔주는 함수
     * @param - selectItemCount(Int), uriCount(Int)
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-30
     **/
    @SuppressLint("ResourceAsColor")
    private fun changeUploadImageColors(selectItemCount: Int, uriCount: Int) {
        if (selectItemCount + uriCount == 20) { // 사이즈가 꽉 차면 빨간색으로 표시
            binding.tvCurrentCountImage.setTextColor(RED)
            binding.tvMiddleCountImage.setTextColor(RED)
            binding.tvTotalCountImage.setTextColor(RED)
        }
    }

    /**
     * @description - 제보하기 페이지 상단 툴 바에 대한 설정 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-25
     */
    private fun settingPostToolBar() {
        setSupportActionBar(binding.postPageToolBar) // 툴 바 설정
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) // 왼쪽 상단 버튼 만들기
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24) // 왼쪽 상단 아이콘
        supportActionBar!!.setDisplayShowTitleEnabled(true) // 툴 바에 타이틀 보이게
    }

    /**
     * @description - 현재 시간을 얻어오는 함수
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-29
     */
    private fun getDateTimeCalendar() {
        val cal = Calendar.getInstance()  // 현재 시간을 얻어오기 위해 캘린더 객체 선언
        day = cal.get(Calendar.DAY_OF_MONTH)
        month = cal.get(Calendar.MONTH)
        year = cal.get(Calendar.YEAR)
        hour = cal.get(Calendar.HOUR_OF_DAY) + 9 // 한국 시간으로 변경
        min = cal.get(Calendar.MINUTE)
        second = 0
    }

    /**
     * @description - 현재 시간을 가져와서 데이터 피커의 값과 옵션을 초기화하여 화면에 띄워주는 함수
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-29
     */
    private fun pickDate() {
        binding.tvEventTimeClicked.setOnClickListener {
            getDateTimeCalendar()
            var datePicker = DatePickerDialog(this, this, year, month, day)
            datePicker.datePicker.maxDate =
                Calendar.getInstance().timeInMillis  // 제보 시 오늘 당일까지만 날짜 선택 가능하도록, 미래의 시간은 선택 불가능하게.
            datePicker.show()
        }
    }

    // 툴 바의 item 선택 이벤트 리스너
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { // 툴 바의 뒤로가기 키가 눌렸을 때 동작
                finish()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // 데이터 피커 선택 완료 콜백 리스너
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        savedDay = dayOfMonth
        savedMonth = month + 1 // 선택한 month 보다 한 달 늦게 할당되는 문제 때문에 1 증가시켜주었음.
        savedYear = year
        getDateTimeCalendar() // 다시 현재 시각 갱신
        TimePickerDialog(this, this, hour, min, false).show()
    }

    // 타임 피커 선택 완료 콜백 리스너
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        savedHour = hourOfDay
        savedMin = minute
        getDateTimeCalendar() // 현재 시간의 초도 가져오기 위함
        binding.tvEventTimeClicked.text = "$savedYear" + String.format(
            "년 %02d월 %02d일 ",
            savedMonth,
            savedDay
        ) + String.format("%02d시 %02d분", savedHour, savedMin) // 서버에 넘겨줘야할 포맷 맞추기
        eventTime = "$savedYear" + String.format(
            "-%02d-%02d ",
            savedMonth,
            savedDay
        ) + String.format("%02d:%02d:%02d", savedHour, savedMin, second)
    }
}