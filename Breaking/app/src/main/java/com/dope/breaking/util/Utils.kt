package com.dope.breaking.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.InputFilter
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.dope.breaking.databinding.ActivitySignUpBinding
import okhttp3.ResponseBody
import java.io.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern


object Utils { // 컴패니언 객체 (Static)

    /**
     * 받아온 미디어 바이트 스트림에 대해서 inputStream 과 outputStream 을 이용해 다운로드 폴더로 쓰기(Write) 진행하는 메소드
     * @param body(ResponseBody) : 응답으로 받아온 미디어 byte 가 들어있는 body
     * @return Boolean : 다운로드 성공 여부를 반환
     * @author Tae hyun Park
     * @since 2022-09-14
     */
    internal fun writeResponseBodyToDisk(body: ResponseBody?, postId: Long): Boolean {
        return try {
            val futureStudioIconFile =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + File.separator + "download${postId}" + ".zip")
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = body?.contentLength()
                var fileSizeDownloaded: Long = 0
                inputStream = body?.byteStream()
                outputStream = FileOutputStream(futureStudioIconFile)
                while (true) {
                    val read: Int = inputStream!!.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()
                }
                outputStream.flush()
                true
            } catch (e: IOException) {
                false
            } finally {
                if (inputStream != null)
                    inputStream.close();
                if (outputStream != null)
                    outputStream.close();
            }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * @description - 해시 태그가 포함된 문자열에서 태그 내용만 추출하여 ArrayList<String>으로 받아오는 함수(띄어쓰기를 해야 인식하므로 Deprecated.)
     * @param - hashTag(String) : 해시태그 입력 필드 문자열 전체
     * @return - ArrayList<String> : #별로 처리된 해시 태그 리스트 반환
     * @author - Tae hyun Park
     * @since - 2022-07-29 | 2022-08-05
     */
    internal fun getArrayHashTag(hashTag: String): ArrayList<String> {
        var hashTagList = ArrayList<String>()
        var contentStringEnter = hashTag.split("\n") // 엔터를 기준으로 나눔
        if (contentStringEnter.isNotEmpty()) { // 엔터를 한 번 이상 눌렀다면
            for (i in contentStringEnter.indices) {
                var splitString = contentStringEnter[i].split(" ") // 한 줄 한 줄 공백을 기준으로 나눔
                for (i in splitString.indices) {
                    if (splitString[i].indexOf("#") == 0) { // #이 있고 인덱스가 0이라면
                        if (splitString[i].length > 1 && splitString[i].count { c -> c == '#' } == 1) // #을 포함하는 단어가 있어야 하고, #은 처음 한 번만 오도록
                            hashTagList.add(splitString[i].replace("#", "")) // 해당 해시태그 문자열을 추출하여 저장
                    }
                }
            }
        } else { // 엔터키를 누르지 않았다면
            var contentString = hashTag.split(" ") // 한 줄을 기준으로 공백을 기준으로 나눔
            for (i in contentString.indices) {
                if (contentString[i].indexOf("#") == 0) { // #이 있고 인덱스가 0이라면
                    if (contentString[i].length > 1 && contentString[i].count { c -> c == '#' } == 1) // #을 포함하는 단어가 있어야 하고, #은 처음 한 번만 오도록
                        hashTagList.add(contentString[i].replace("#", "")) // 해당 해시태그 문자열을 추출하여 저장
                }
            }
        }
        return hashTagList
    }

    /**
     * @description - 해시 태그가 포함된 문자열에서 태그 내용만 추출하여 ArrayList<String>으로 받아오는 함수(띄어쓰기가 없이도 인식이 가능하도록 구현한 함수)
     * @param - hashTag(String) : 해시태그 입력 필드 문자열 전체
     * @return - ArrayList<String> : #별로 처리된 해시 태그 리스트 반환
     * @author - Tae hyun Park
     * @since - 2022-08-23
     */
    internal fun getArrayHashTagWithOutSpace(hashTag: String): ArrayList<String> {
        var hashTagList = ArrayList<String>()
        var contentStringEnter = hashTag.split("\n") // 가장 먼저, 엔터 즉 줄바꿈을 기준으로 나눔
        if (contentStringEnter.isNotEmpty()) {
            for (i in contentStringEnter.indices) { // 리스트가 비어 있지 않으면서, 한 줄이라면 1개의 리스트가 들어가고, 그 이상이라면 여러 개가 들어감.
                var splitStringTag = contentStringEnter[i].split("#") // 한 줄 한 줄 샵(#)을 기준으로 나눔
                if (splitStringTag.size > 1) { // #이 하나라도 있다면 전처리 시작
                    for (i in splitStringTag.indices) {
                        if (i > 0) { // #을 기준으로 나눠지는 앞에 오는 첫 번째 리스트는 해시태그에 포함되지 않으므로 제외
                            var splitStringSpace = splitStringTag[i].split(" ") // 각각에 대해 공백으로 나눈다.
                            for (j in splitStringSpace.indices) { // 리스트의 첫 번째 인덱스 값을 다시 해시 태그 값으로 취한다.
                                if (j == 0 && splitStringSpace[j].isNotEmpty()) {
                                    Log.d("Utils.kt", "해시 태그 값 : ${splitStringSpace[j]}")
                                    hashTagList.add(splitStringSpace[j])
                                }
                            }
                        }
                    }
                }
            }
        }
        return hashTagList
    }

    /**
     * @description - 게시글 생성 날짜 문자열의 초 부분 포맷이 매칭이 모두 ssssss로 안되는 문제가 있어 sssss의 경우도 고려해주는 함수
     * @param - checkDate(String) : 게시글 생성 날짜 문자열
     * @return - Boolean : 날짜 형식에 따른 true false 값 반환
     * @author - Tae hyun Park
     * @since - 2022-08-22
     */
    internal fun checkDate(checkDate: String): Boolean {
        return try {
            val formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS") // exception이 난다면 SSSSS 형식으로 맞춰주면 됨.
            val localStartDateTime = LocalDateTime.parse(checkDate, formatter)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * @description - 현재 시간을 포맷에 맞게 스트링으로 가져오는 함수, flag 에 따라 Format 을 다르게 가져온다.
     * @param - None
     * @return - String
     * @author - Tae hyun Park
     * @since - 2022-07-29 | 2022-08-11
     */
    internal fun getCurrentTime(flag: Boolean): String {
        var now = System.currentTimeMillis()
        var date = Date(now)
        var simpleFormat: SimpleDateFormat = if (flag)
            SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        else
            SimpleDateFormat("yyyy년 MM월 dd일 hh시 mm분 ss초")
        return simpleFormat.format(date)
    }

    /**
     * @description - 닉네임 입력 시 원하는 정규식에 맞게 입력 가능하도록 하는 함수
     * @param - 정규식 form, 입력 문자열 길이, 컨텍스트
     * @return - Array<InputFilter>
     * @author - Tae hyun Park
     * @since - 2022-07-08
     **/
    internal fun regularExpressionNickname(
        form: String,
        length: Int,
        context: Context
    ): Array<InputFilter> {
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
     * @description - 앱 내에서 특정 사이즈로 이미지를 보여주어야 할 때 사용하는 메소드
     * @param - context, uri, binding, width, height
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-11
     */
    internal fun setImageWithGlide(
        context: Context,
        uri: Uri?,
        binding: ActivitySignUpBinding,
        width: Int,
        height: Int
    ) {
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .circleCrop()
            .override(width, height)
            .into(binding.imgBtnProfileImage)
    }

    /**
     * @description - 식별값 Uri 로 Glide 를 통해 이미지 비트맵을 가져오는 메소드
     * @param - context, uri
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-07-11
     */
    internal fun getBitmapWithGlide(context: Context, uri: Uri?, handler: Handler) {
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    // 글라이드를 통해 uri 로 비트맵을 얻어오고, 이를 메인 스레드의 비트맵 전역변수로 전달
                    val message = handler.obtainMessage()
                    val bundle: Bundle = Bundle()

                    bundle.putParcelable("Bitmap", resource)
                    message.data = bundle
                    handler.sendMessage(message)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    TODO("Not yet implemented")
                }
            })
    }

    /**
     * @description - uri로 선택한 이미지 파일에 대한 파일명을 가져오는 함수
     * @param - Uri
     * @return - String?
     * @author - Tae hyun Park
     * @since - 2022-07-09 | 2022-07-13
     */
    internal fun getFileNameFromURI(
        uri: Uri,
        contentResolver: ContentResolver
    ): String? { // 추가적인 모듈화 가능할듯?
        var buildName = Build.MANUFACTURER
        if (buildName.equals("Xiaomi")) {
            return uri.path
        }
        var cursor = contentResolver.query(uri, null, null, null, null)
        var nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) // 파일명 반환
        cursor!!.moveToFirst() // 커서 위치 이동
        return cursor.getString(nameIndex!!)
    }

    /**
     * @description - uri 를 통해 선택한 파일 인스턴스를 얻어오는 함수
     * @param - Uri, ContentResolver
     * @return - File
     * @author - Tae hyun Park
     * @since - 2022-09-05
     */
    internal fun getFileFromURI(
        uri: Uri,
        contentResolver: ContentResolver
    ): File {
        // 이미지/영상에 따라 미디어 타입을 구분
        var filePathColumn: Array<String> =
            if (uri.toString().contains("image")) {
                arrayOf(MediaStore.Video.Media.DATA)
            } else {
                arrayOf(MediaStore.Images.Media.DATA)
            }

        var cursor = contentResolver.query(uri, filePathColumn, null, null, null)
        cursor!!.moveToFirst() // 커서 위치 이동
        var nameIndex = cursor?.getColumnIndex(filePathColumn[0]) // 파일명 반환
        var fileString = cursor?.getString(nameIndex!!)
        cursor!!.close()
        return File(fileString)
    }
}