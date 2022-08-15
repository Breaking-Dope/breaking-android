package com.dope.breaking.util

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import com.dope.breaking.R
import com.dope.breaking.databinding.CustomDialogFilterOptionBinding
import com.dope.breaking.databinding.CustomDialogSortOptionBinding
import java.util.*

/**
 * AlertDialog 에 대한 Util 클래스
 * 원하는 Dialog 를 inner class 로 커스터마이징하여 구현함으로써 간편하게 사용
 */
class DialogUtil {
    /**
     * 텍스트 컨텐츠 하나와 단일 버튼으로 이루어진 Dialog
     * @param context(Context): 이 Dialog 가 보여지고자 하는 컨텍스트(위치)
     * @param content(String): 컨텐츠 텍스트 문자열
     * @param buttonText(String): 버튼 텍스트 문자열
     * @param event(() -> Unit): 버튼 클릭 시 실행할 함수
     */
    inner class SingleDialog(
        context: Context,
        private val content: String,
        private val buttonText: String,
        private val event: () -> Unit = { }
    ) : Dialog(context) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContentView(R.layout.custom_dialog_single_button)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(false)

            val textView = findViewById<TextView>(R.id.tv_dialog_content)
            textView.text = content

            val button = findViewById<Button>(R.id.btn_dialog_single_ok)
            button.text = buttonText
            button.setOnClickListener {
                event()
                dismiss()
            }
        }
    }

    /**
     * 텍스트 컨텐츠 하나와 두개의 버튼으로 이루어진 Dialog
     * @param context(Context): 이 Dialog 가 보여지고자 하는 컨텍스트
     * @param leftText(String): 왼쪽 버튼 텍스트
     * @param rightText(String): 오른쪽 버튼 텍스트
     * @param leftEvent(()-> Unit)): 왼쪽 버튼 클릭 시 실행할 함수
     * @param rightEvent(()-> Unit)): 오른쪽 버튼 클릭 시 실행할 함수
     * @param allowCancel(Boolean): 외부 영역 클릭 시 종료 허용 여부
     */
    inner class MultipleDialog(
        context: Context,
        private val content: String,
        private val leftText: String,
        private val rightText: String,
        private val leftEvent: () -> Unit = {},
        private val rightEvent: () -> Unit = {},
        private val allowCancel: Boolean = false
    ) : Dialog(context) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.custom_dialog_multiple_button)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(allowCancel)

            val textView = findViewById<TextView>(R.id.tv_dialog_content)
            textView.text = content

            val leftButton = findViewById<Button>(R.id.btn_dialog_multi_left)
            leftButton.text = leftText
            leftButton.setOnClickListener {
                leftEvent()
                dismiss()
            }

            val rightButton = findViewById<Button>(R.id.btn_dialog_multi_right)
            rightButton.text = rightText
            rightButton.setOnClickListener {
                rightEvent()
                dismiss()
            }
        }
    }

    /**
     * 메인 피드에서 "정렬" 옵션에 대한 다이얼로그
     * @param context(Context): Dialog 가 보여지고자 하는 컨텍스트
     * @param applyEvent(() -> Unit): "적용" 버튼 클릭 시 실행되는 이벤트 람다 함수
     */
    inner class SortOptionDialog(
        context: Context,
        private val applyEvent: () -> Unit
    ) : Dialog(context) {
        private lateinit var binding: CustomDialogSortOptionBinding
        var sortIndex = 0 // 최종 선택한 라디오 버튼 인덱스

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            binding = CustomDialogSortOptionBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // 다이얼로그 크기 조정
            val window = window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            binding.radioGroupSortOption.check(R.id.radio_btn_sort_latest) // default 로 "최신순" 체크

            // 라디오 버튼 체크 이벤트 리스너
            binding.radioGroupSortOption.setOnCheckedChangeListener { group, checkId ->
                when (checkId) {
                    R.id.radio_btn_sort_latest -> sortIndex = 0 // 최신순 선택 시
                    R.id.radio_btn_sort_like -> sortIndex = 1 // 좋아요순 선택 시
                    R.id.radio_btn_sort_view -> sortIndex = 2 // 조회수순 선택 시
                }
            }

            // 적용 버튼 클릭 시 이벤트
            binding.btnSortApply.setOnClickListener {
                applyEvent()
                dismiss()
            }

            // "X" 버튼 클릭 시 이벤트
            binding.btnBackClear.setOnClickListener {
                dismiss() // 종료
            }
        }
    }

    /**
     * 메인 피드에서 "필터" 옵션에 대한 다이얼로그
     * @param context(Context): Dialog 가 보여지고자 하는 컨텍스트
     * @param applyEvent(() -> Unit): "적용" 버튼 클릭 시 실행되는 이벤트 람다 함수
     */
    inner class FilterOptionDialog(
        context: Context,
        private val applyEvent: () -> Unit
    ) : Dialog(context), DatePickerDialog.OnDateSetListener {
        var sellState = 0 // 판매 제보 카테고리 라디오 버튼 최종 선택 인덱스
        var lastMin = -1 // 최근 N분 입력한 값
        var startDate: String = "-" // 시작 날짜 최종 선택한 문자열
        var endDate: String = "-" // 종료 날짜 최종 선택한 문자열
        private var dateState = true // 시작 날짜인지 종료 날짜인지 구분 (true: 시작, false: 종료)
        private lateinit var binding: CustomDialogFilterOptionBinding
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            binding = CustomDialogFilterOptionBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // 다이얼로그 크기 조정
            val window = window
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            initOptions() // 초기 상태 설정

            // 최근 N분 입력창에 대한 텍스트 감지
            val textWatcher = object : TextWatcher {
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    if (p0.toString().isNotEmpty()) { // 현재 텍스트가 입력되어 있다면
                        // 기간 옵션 비활성화 처리
                        binding.btnStartDate.isEnabled = false
                        binding.btnEndDate.isEnabled = false
                        binding.btnStartDate.setBackgroundResource(R.drawable.button_disable_background)
                        binding.btnEndDate.setBackgroundResource(R.drawable.button_disable_background)
                        binding.tvStartDate.text = "-"
                        binding.tvEndDate.text = "-"
                        binding.tvStartDateTitle.setTextColor(context.getColor(R.color.sign_up_input_hint_text_color))
                        binding.tvEndDateTitle.setTextColor(context.getColor(R.color.sign_up_input_hint_text_color))
                    } else { // 현재 텍스트가 입력되어 있지 않다면
                        // 기간 옵션 활성화 처리
                        binding.btnStartDate.isEnabled = true
                        binding.btnEndDate.isEnabled = true
                        binding.btnStartDate.setBackgroundResource(R.drawable.theme_color_round_button)
                        binding.btnEndDate.setBackgroundResource(R.drawable.theme_color_round_button)
                        binding.tvStartDate.text = "-"
                        binding.tvEndDate.text = "-"
                        binding.tvStartDateTitle.setTextColor(context.getColor(R.color.black))
                        binding.tvEndDateTitle.setTextColor(context.getColor(R.color.black))
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(p0: Editable?) {
                }
            }

            binding.etFilterLatestMin.addTextChangedListener(textWatcher) // EditText 텍스트 감지 리스너 지정

            // 시작 날짜 선택 이미지 버튼 클릭 시
            binding.btnStartDate.setOnClickListener {
                dateState = true // 시작 날짜 상태로 전환
                val today = Calendar.getInstance() // 현재 날짜 가져오기
                val datePicker = DatePickerDialog(
                    context,
                    this@FilterOptionDialog,
                    today.get(Calendar.YEAR),
                    today.get(Calendar.DAY_OF_MONTH),
                    today.get(Calendar.MONTH)
                ) // DatePicker 객체 생성
                datePicker.datePicker.maxDate = today.timeInMillis // 최대 선택 가능한 날짜를 오늘로 설정
                datePicker.show() // DatePicker 실행
            }

            // 종료 날짜 선택 이미지 버튼 클릭 시
            binding.btnEndDate.setOnClickListener {
                dateState = false // 종료 날짜 상태로 전환
                val today = Calendar.getInstance() // 현재 날짜 가져오기
                val datePicker = DatePickerDialog(
                    context,
                    this@FilterOptionDialog,
                    today.get(Calendar.YEAR),
                    today.get(Calendar.DAY_OF_MONTH),
                    today.get(Calendar.MONTH)
                ) // DatePicker 객체 생성
                datePicker.datePicker.maxDate = today.timeInMillis // 최대 선택 가능한 날짜를 오늘로 설정
                datePicker.show() // DatePicker 실행
            }

            // 뒤로가기 버튼 클릭 시
            binding.btnBackClear.setOnClickListener {
                dismiss() // 종료
            }
            // 초기화 버튼 클릭 시
            binding.btnFilterReset.setOnClickListener {
                initOptions() // 초기 상태로 전환
            }

            // 적용 버튼 클릭 시
            binding.btnFilterApply.setOnClickListener {
                if (binding.etFilterLatestMin.text.toString().isNotEmpty()) // 최근 N분 입력했을 경우
                    lastMin = binding.etFilterLatestMin.text.toString().toInt() // 입력한 값 저장

                // 시작 날짜와 종료 날짜 둘 중 하나 선택하지 않았을 때 defensive 처리
                if ((startDate != "-" && endDate == "-") || (startDate == "-" && endDate != "-")) {
                    Toast.makeText(context, "나머지 날짜를 선택하세요!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                applyEvent() // 이벤트 함수 실행
                dismiss()
            }

            // 판매 제보 라디오 버튼 체크 이벤트
            binding.radioGroupFilterSell.setOnCheckedChangeListener { group, checkId ->
                when (checkId) {
                    R.id.radio_btn_filter_entire -> sellState = 0 // 전체 선택 시
                    R.id.radio_btn_filter_sold -> sellState = 1 // 판매 제보만 선택 시
                    R.id.radio_btn_filter_unsold -> sellState = 2 // 미판매된 제보만 선택 시
                }
            }
        }

        /**
         * DatePicker 날짜 선택 시 콜백 함수
         */
        override fun onDateSet(p0: DatePicker?, year: Int, month: Int, day: Int) {
            binding.etFilterLatestMin.inputType = InputType.TYPE_NULL // 최근 N분 입력창 막기
            binding.etFilterLatestMin.hint = "" // "힌트 텍스트 제거"
            binding.etFilterLatestMin.setBackgroundResource(R.drawable.disable_input_background) // 배경색 변경

            if (dateState) { // 시작 날짜에 대한 DatePicker 인 경우
                startDate = "$year${String.format("-%02d-%02d", month + 1, day)}"
                binding.tvStartDate.text = startDate
            } else { // 종료 날짜에 대한 DatePicker 인 경우
                endDate = "$year${String.format("-%02d-%02d", month + 1, day)}"
                binding.tvEndDate.text = endDate
            }
        }

        /**
         * 가장 초기 상태의 다이얼로그로 초기화하는 함수
         * @author Seunggun Sin
         * @since 2022-08-15
         */
        private fun initOptions() {
            binding.tvStartDate.text = "-"
            startDate = "-"
            endDate = "-"
            lastMin = -1
            sellState = 0
            binding.tvEndDate.text = "-"
            binding.etFilterLatestMin.setText("")
            binding.etFilterLatestMin.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etFilterLatestMin.setBackgroundResource(R.drawable.enable_input_background)
            binding.radioGroupFilterSell.check(R.id.radio_btn_filter_entire)
        }
    }

    /**
     * 로딩창에 사용하기위한 Progress Dialog
     * @param activity(Activity): Dialog 가 실행되는 액티비티
     */
    inner class ProgressDialog(private val activity: Activity) {
        private lateinit var dialog: AlertDialog

        /**
         * 로딩 창을 시작하는 함수
         * @author Seunggun Sin
         * @since 2022-07-18 | 2022-07-19
         */
        fun showDialog() {
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater
            builder.setView(inflater.inflate(R.layout.progress_dialog_layout, null))
            dialog = builder.create()

            dialog.setCanceledOnTouchOutside(false) // 외부 터치에 의한 Dialog 종료 방지
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Dialog 내용 자체를 투명하게
            dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) // 외부 배경을 투명하게

            dialog.show()
        }

        /**
         * 로딩 창을 종료하는 함수 (showDialog 함수가 먼저 호출이 되어야 함. 그렇지 않으면 Exception 발생)
         * @author Seunggun Sin
         * @since 2022-07-18 | 2022-07-19
         */
        fun dismissDialog() {
            try {
                dialog.dismiss()
            } catch (e: UninitializedPropertyAccessException) {
                e.printStackTrace()
            }
        }

        /**
         * 현재 로딩 창이 보여지고 있는지 없는지 체크해주는 함수
         * @return 보여지고 있으면 true, 아니면 false
         * @author Seunggun Sin
         * @since 2022-07-19
         */
        fun isShowing(): Boolean {
            return try {
                dialog.isShowing
            } catch (e: UninitializedPropertyAccessException) {
                false
            }
        }
    }
}