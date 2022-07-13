package com.dope.breaking.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.InputFilter
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.dope.breaking.databinding.ActivitySignUpBinding
import java.util.regex.Pattern


object Util { // 컴패니언 객체 (Static)
    /**
    @description - 닉네임 입력 시 원하는 정규식에 맞게 입력 가능하도록 하는 함수
    @param - 정규식 form, 입력 문자열 길이, 컨텍스트
    @return - Array<InputFilter>
    @author - Tae hyun Park
    @since - 2022-07-08
     **/
    internal fun RegularExpressionNickname(form : String, length : Int, context : Context) : Array<InputFilter>{
        return arrayOf(InputFilter { source, _, _, _, _, _ ->
            val ps: Pattern =
                Pattern.compile(form) // 한글, 숫자, 영문만 가능하도록 설정
            if (source == "" || ps.matcher(source).matches()) {
                return@InputFilter source
            }
            Toast.makeText(context, "한글, 영문, 숫자만 입력 가능합니다.", Toast.LENGTH_SHORT).show()
            ""
        }, InputFilter.LengthFilter(length))
    }

    /**
    @description - 앱 내에서 특정 사이즈로 이미지를 보여주어야 할 때 사용하는 메소드
    @param - context, uri, binding, width, height
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-11
     */
    internal fun setImageWithGlide(context: Context, uri: Uri?, binding: ActivitySignUpBinding, width: Int, height: Int){
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .circleCrop()
            .override(width,height)
            .into(binding.imgBtnProfileImage)
    }

    /**
    @description - 식별값 Uri 로 Glide 를 통해 이미지 비트맵을 가져오는 메소드
    @param - context, uri
    @return - None
    @author - Tae hyun Park
    @since - 2022-07-11
     */
    internal fun getBitmapWithGlide(context: Context, uri: Uri?, handler: Handler){
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    // 글라이드를 통해 uri 로 비트맵을 얻어오고, 이를 메인 스레드의 비트맵 전역변수로 전달
                    val message = handler.obtainMessage()
                    val bundle: Bundle = Bundle()

                    bundle.putParcelable("Bitmap",resource)
                    message.data = bundle
                    handler.sendMessage(message)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    TODO("Not yet implemented")
                }
            })
    }

    /**
    @description - 이미지 실제 경로 반환
    @param - Uri
    @return - String?
    @author - Tae hyun Park
    @since - 2022-07-09 | 2022-07-12
     */
    internal fun getRealPathFromURI(uri: Uri, contentResolver: ContentResolver): String? { // 추가적인 모듈화 가능할듯?
        var buildName = Build.MANUFACTURER
        if (buildName.equals("Xiaomi")) {
            return uri.path
        }
        var columnIndex = 0
        var proj = arrayOf(MediaStore.Images.Media.DATA)
        var cursor = contentResolver.query(uri, proj, null, null, null)
        if (cursor!!.moveToFirst()) {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        }
        return cursor.getString(columnIndex)
    }
}