package com.dope.breaking.util

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.dope.breaking.R

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