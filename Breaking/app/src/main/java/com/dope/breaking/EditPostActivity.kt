package com.dope.breaking

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet
import com.dope.breaking.databinding.ActivityPostBinding
import com.dope.breaking.exception.FailedGetLocationException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.map.KakaoMapActivity
import com.dope.breaking.model.LocationList
import com.dope.breaking.model.PostLocation
import com.dope.breaking.model.request.RequestPostDataModify
import com.dope.breaking.model.response.ResponsePostDetail
import com.dope.breaking.model.response.ResponsePostUpload
import com.dope.breaking.post.PostManager
import com.dope.breaking.post.ValidationPost
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

class EditPostActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
TimePickerDialog.OnTimeSetListener {

    private val TAG = "EditPostActivity.kt" // Tag Log
    private var mbinding: ActivityPostBinding? = null
    private val binding get() = mbinding!!

    private lateinit var locationActivityResult: ActivityResultLauncher<Intent> // 카카오 맵으로부터 위치를 선택하고 돌아온다면 그 후 처리를 위한 activityResult

    private lateinit var locationData: LocationList // 받아올 제보 위치 DTO

    private lateinit var hashTagList: ArrayList<String> // 해시 태그 리스트

    private lateinit var eventTime: String // 제보 발생 시간

    private lateinit var getPostInfo: ResponsePostDetail // 수정하기 페이지로 받아온 기존 게시물 정보

    private var isPostTypeSelected: String =
        "charged" // 제보 방식으로, 단독, 유료, 무료 제보로 나뉘고 기본값은 유료 제보로 설정.

    private var isAnonymousSelected: Boolean = false // 익명 여부로, true-> 익명, false-> 공개이고 기본값은 공개로 설정.

    private var postPriceString: String = "" // 제보 가격 콤마 표시를 위한 저장 변수

    private var decimalFormat = DecimalFormat("#,###") // 제보 가격 콤마 표시를 위한 포맷

    private var getPostId = -1 // 수정 할 게시글 id

    private lateinit var progressDialog : DialogUtil.ProgressDialog // 요청 로딩 다이얼로그

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

        // 카카오 지도에서 제보 위치를 선택하고 제보 페이지로 다시 돌아오면
        locationActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    locationData =
                        it.data?.getSerializableExtra("locationData") as LocationList // 선택한 장소 위치 정보 받아와 할당
                    binding.tvLocationShow.text = locationData.address // 선택한 위치 정보 표시
                }
            }

        binding.etPostPrice.addTextChangedListener(textWatcher) // 제보 가격 콜백 함수 등록

        settingPostToolBar()  // 툴 바 설정
        allowScrollEditText() // EditText 스크롤 터치 이벤트 허용
        pickDate() // 제보 이벤트 발생 시간 선택을 위한 데이트, 타임 피커
        clickPostPageButtons() // 제보하기 페이지들의 버튼 이벤트 함수
        setCurrentLocation() // 현재 위치 설정 함수

        // 제보하기 페이지를 수정 페이지에 맞게 수정
        binding.postPageToolBar.title = "수정하기"
        binding.layoutImageButton.visibility = View.GONE
        binding.viewRecyclerImage.visibility = View.GONE
        binding.tvRecyclerImage.visibility = View.VISIBLE
        binding.btnPostRegisterBtn.text = "수정하기"
        // TextView visible 처리 후 하위 divider 와 유연하게 연결
        val constraintLayout = binding.viewRoot
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(binding.divider1.id, ConstraintSet.TOP, binding.tvRecyclerImage.id, ConstraintSet.BOTTOM)
        constraintSet.connect(binding.divider1.id, ConstraintSet.LEFT, binding.tvRecyclerImage.id, ConstraintSet.LEFT)
        constraintSet.connect(binding.divider1.id, ConstraintSet.RIGHT, binding.tvRecyclerImage.id, ConstraintSet.RIGHT)
        constraintSet.applyTo(constraintLayout)

        // 받아온 인텐트 값을 바탕으로 뷰에 데이터 뿌려주기
        if(intent != null){
            getPostInfo = intent.getSerializableExtra("postInfo") as ResponsePostDetail
            getPostId = intent.getIntExtra("postId",-1)

            // 제보 발생 시간 기본 할당
            var postTimeList = getPostInfo.eventDate.split("T").toTypedArray()
            var postFirstList = postTimeList[0].split("-").toTypedArray()
            var postSecondList = postTimeList[1].split(":").toTypedArray()
            binding.tvEventTimeClicked.text = "${postFirstList[0]}년 ${postFirstList[1]}월 ${postFirstList[2]}일 ${postSecondList[0]}시 ${postSecondList[1]}분"
            eventTime = "${postFirstList[0]}-${postFirstList[1]}-${postFirstList[2]} ${postSecondList[0]}:${postSecondList[1]}:00"

            // 위치 주소 보여주기
            binding.tvLocationShow.text = getPostInfo.location.address
            // 제보 제목
            binding.etTitle.setText(getPostInfo.title)
            // 제보 내용
            binding.etContent.setText(getPostInfo.content)
            // 제보 방식
            when(getPostInfo.postType){
                "CHARGED" -> {
                    isPostTypeSelected = "charged"
                    binding.btnPostTypeCharged.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 유료 제보 색상 활성화
                    binding.btnPostTypeFree.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 무료 제보 색상 비활성화
                    binding.btnPostTypeExclusive.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 단독 제보
                }"FREE" -> {
                    isPostTypeSelected = "free"
                    binding.btnPostTypeFree.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 무료 제보 색상 활성화
                    binding.btnPostTypeCharged.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 유료 제보 색상 비활성화
                    binding.btnPostTypeExclusive.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 단독 제보 색상 비활성화
                    binding.etPostPrice.isEnabled = false // 사용자로부터 가격 설정 불가능하도록 설정
                }"EXCLUSIVE" -> {
                    isPostTypeSelected = "exclusive"
                    binding.btnPostTypeExclusive.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 단독 제보 색상 활성화
                    binding.btnPostTypeFree.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 무료 제보 색상 비활성화
                    binding.btnPostTypeCharged.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 유료 제보 선택 색상 비활성화
                }
            }
            // 제보 가격
            binding.etPostPrice.setText(getPostInfo.price.toString())
            // 프로필 공개 여부
            when(getPostInfo.isAnonymous){
                true -> {
                    isAnonymousSelected = true
                    binding.btnPostProfileTypeSecret.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 익명 버튼 색상 활성화
                    binding.btnPostProfileTypePublic.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 공개 버튼 색상 비활성화
                }false -> {
                    isAnonymousSelected = false
                    binding.btnPostProfileTypePublic.setBackgroundResource(R.drawable.sign_up_user_type_selected) // 공개 버튼 색상 활성화
                    binding.btnPostProfileTypeSecret.setBackgroundResource(R.drawable.sign_up_user_type_unselected) // 익명 버튼 색상 비활성화
                }
            }
        }
    }

    /**
     * @description - 제보 수정하기 페이지 클릭 관련 콜백 함수를 모아놓은 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-04
     */
    private fun clickPostPageButtons() {
        // 게시물 수정 요청 에러 시 띄워줄 다이얼로그 정의
        val requestErrorDialog =
            DialogUtil().SingleDialog(applicationContext, "게시글 수정에 문제가 발생하였습니다.", "확인")
        // 요청 Jwt 토큰 가져오기
        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(applicationContext).getAccessTokenFromLocal()
        hashTagList = ArrayList<String>() // 해시 태그 리스트 초기화

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

        // 수정하기 버튼을 누르면
        binding.btnPostRegisterBtn.setOnClickListener {
            // 해시 태그 값이 있는지 확인, 태그가 없다면 기본 값으로 다시 초기화
            if (binding.etContent.text.toString().indexOf('#') !== -1)
                hashTagList = Utils.getArrayHashTagWithOutSpace(binding.etContent.text.toString()) // 해시 태그 처리하여 태그 문자열 추출
            else
                hashTagList.clear() // 해시 태그 값이 없으면 리스트 재 초기화

            // 필드 검증
            val validationPost = ValidationPost()
            if (validationPost.startPostValidation(binding)) { // 필드 검증이 성공적이라면
                // 요청 DTO 생성
                val requestPostEditData = RequestPostDataModify(
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
                )

                // 게시글 수정 요청 함수
                processPostEdit(
                    token,
                    getPostId.toLong(),
                    requestPostEditData,{
                        // 게시글 작성 요청 다이얼 로그 시작
                        progressDialog = DialogUtil().ProgressDialog(this)
                        progressDialog.showDialog()
                    },{
                        if (progressDialog.isShowing()) { // 로딩 다이얼로그 종료
                            progressDialog.dismissDialog()
                        }
                        intent.putExtra("postId",it.postId.toInt()) // 세부 조회 페이지로 수정 완료된 postId 전달
                        setResult(RESULT_OK, intent)
                        finish() // 다시 세부 조회 페이지로 이동
                    },{
                        it.printStackTrace()
                        requestErrorDialog.show()
                    }
                )
            }
        }
    }

    /**
     * @description - 게시물 수정 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @param - postId(Long) : 수정 요청할 게시물 id
     * @param - postInfo(RequestPostData) : 수정이 완료된 게시물 id
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-04
     */
    private fun processPostEdit(
        token: String,
        postId: Long,
        postInfo: RequestPostDataModify,
        init: () -> Unit,
        last: (ResponsePostUpload) -> Unit,
        error: (ResponseErrorException) -> Unit
    ){
        init()
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager()
            try {
                val response = postManager.startEditPost(
                    token,
                    postId,
                    postInfo
                )
                last(response)
            }catch (e: ResponseErrorException){
                error(e)
            }
        }
    }

    /**
     * @description - 제보 수정하기 페이지에서 기본값으로 현재 자신의 위치를 설정하는 함수, 위도 경도와 주소를 받아오고, locationData 에 할당하는 함수
     * @param - None
     * @return - None
     * @author - Tae Hyun Park
     * @since - 2022-09-04
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
     * @description - 현재 시간을 얻어오는 함수
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-04
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
     * @since - 2022-09-04
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

    /**
     * @description - 제보 수정하기 페이지 상단 툴 바에 대한 설정 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-04
     */
    private fun settingPostToolBar() {
        setSupportActionBar(binding.postPageToolBar) // 툴 바 설정
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) // 왼쪽 상단 버튼 만들기
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24) // 왼쪽 상단 아이콘
        supportActionBar!!.setDisplayShowTitleEnabled(true) // 툴 바에 타이틀 보이게
    }

    /**
     * @description - activity_edit_post.xml에서 이미 최상위 NestedScrollView가 정의되어 있기 때문에 EditText에 스크롤 옵션을 주어도 이벤트가 막히는 현상이 발생한다.
    따라서 해당 메소드를 통해 EditText 가 터치되어있을 때 부모의 스크롤 권한을 가로채고, EditText가 아닌 바깥을 터치한다면 다시 부모 스크롤 뷰가 동작하도록
    하는 메소드.
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-04
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