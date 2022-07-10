package com.dope.breaking.signup

import android.graphics.Bitmap
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.RequestSignUp
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import kotlin.jvm.Throws

class Register {
    private val multiPartFileName = "profileImg" // multi part 요청에 대한 key 이름

    /**
     * input 으로 받아온 회원가입 정보를 바탕으로 Request Body 생성 및 회원가입 요청하는 메소드
     * @param inputData(RequestSignUp): 회원가입 필드에 대한 데이터 클래스 객체
     * @param imageData(Bitmap): 프로필 이미지에 대한 bitmap 데이터
     * @param imageName(String): 프로필 이미지의 파일 이름
     * @throws ResponseErrorException: 정상 응답 (2xx) 이외의 응답이 왔을 때 exception 발생
     * @return Headers: 응답에 대한 헤더 전체 데이터
     * @author - Seunggun Sin
     * @since - 2022-07-08 | 2022-07-09
     */
    @Throws(ResponseErrorException::class) // @Throws 어노테이션 사용 (Java 에서의 throws 키워드)
    fun startRequestSignUp(
        inputData: RequestSignUp,
        imageData: Bitmap,
        imageName: String
    ): Headers {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)
        // 이미지에 대한 RequestBody 생성 (image/*)
        val imageRequestBody =
            RequestBody.create(MediaType.parse("image/*"), convertBitmapToByte(imageData))
        // 이미지에 대한 RequestBody 를 바탕으로 Multi form 데이터 생성
        val imgFile =
            MultipartBody.Part.createFormData(multiPartFileName, imageName, imageRequestBody)
        // 나머지 필드에 대한 RequestBody 생성 (text/plain)
        val data =
            RequestBody.create(MediaType.parse("text/plain"), inputData.convertJsonToString())

        // retrofit 이용하여 회원가입 요청
        val response = service.requestSignUp(imgFile, data)

        if (response.code() in 200..299) { // 요청이 성공적이라면
            return response.headers() // 응답의 헤더 객체 리턴
        } else { // 실패했다면
            // 예외 던지기
            throw ResponseErrorException("요청에 실패하였습니다. error: ${response.errorBody().toString()}")
        }
    }

    /**
     * Bitmap 데이터를 Byte Array 형태로 변환 (이미지 compression 포함)
     * @param bitmap(Bitmap): Bitmap 타입의 이미지 데이터
     * @param quality(Int): 이미지의 품질 (%) - default=100
     * @return - ByteArray: 변환된 byte array
     * @author - Seunggun Sin
     * @since - 2022-07-09
     */
    private fun convertBitmapToByte(bitmap: Bitmap, quality: Int = 100): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream) // compression 및 변환
        return byteArrayOutputStream.toByteArray()
    }

}