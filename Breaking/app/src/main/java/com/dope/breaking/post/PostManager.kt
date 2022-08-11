package com.dope.breaking.post

import android.graphics.Bitmap
import android.util.Log
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.request.RequestPostData
import com.dope.breaking.model.response.ResponsePostUpload
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.ValueUtil
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import kotlin.jvm.Throws

class PostManager {
    private val TAG = "PostManager.kt"
    /**
     * input 으로 받아온 제보 게시글 정보를 바탕으로 Request Body 생성 및 게시글 작성 요청하는 메소드
     * @param inputData(RequestPostData): 제보글 작성 필드에 대한 데이터 클래스 객체
     * @param imageData(Bitmap): 제보 미디어에 대한 bitmap 데이터
     * @param imageName(String): 제보 미디어의 파일 이름
     * @throws ResponseErrorException: 정상 응답 (2xx) 이외의 응답이 왔을 때 exception 발생
     * @return ResponsePostUpload: 응답 바디로 오는 postId를 담아주기 위한 클래스 타입
     * @author Tae hyun Park
     * @since 2022-08-02
     */
    @Throws(ResponseErrorException::class)
    suspend fun startPostUpload(
        inputData: RequestPostData,
        imageData: ArrayList<Bitmap>,
        imageName: ArrayList<String>,
        token : String
    ): ResponsePostUpload{
        // MultiPart.Body List 선언 및 초기화
        var multipartList = ArrayList<MultipartBody.Part>()

        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 받아온 미디어의 사이즈만큼 반복하여 multipart List 생성
        for(i in 0 until imageData.size){
            if (imageData.size == 0) break // 받아온 미디어가 없으면 반복문 탈출
            // 이미지에 대한 RequestBody 생성 (image/* video/*), 영상의 경우 아직 바이너리로 처리되지 않음.
            val imageRequestBody =
                RequestBody.create(MediaType.parse("image/* video/*"), convertBitmapToByte(imageData.get(i)))
            // 이미지에 대한 RequestBody 를 바탕으로 Multi form 데이터 리스트 생성
            multipartList.add(
                MultipartBody.Part.createFormData(
                    ValueUtil.MULTIPART_POST_KEY,
                    imageName.get(i),
                    imageRequestBody
                )
            )
        }
        // 나머지 필드에 대한 RequestBody 생성 (text/plain)
        val objectMapper = ObjectMapper()
        val result = objectMapper.writeValueAsString(inputData)

        val data =
            RequestBody.create(MediaType.parse("text/plain"), result) // inputData.convertJsonToString()

        Log.d(TAG, "json 테스트1 : "+inputData.convertJsonToString())
        Log.d(TAG, "json 테스트2 : ${result}")

        // retrofit 이용하여 게시글 작성 요청
        val response = service.requestPostUpload(
            ValueUtil.JWT_REQUEST_PREFIX + token,
            multipartList ?: null, // 미디어 리스트가 없으면 서버로 null 전송
            data
        )

        if (response.code() in 200..299) { // 요청이 성공적이라면
            return response.body()!! // 응답의 response 객체 리턴
        } else { // 실패했다면
            // 예외 던지기
            throw ResponseErrorException("요청에 실패하였습니다. error: ${response.errorBody()?.string()}")
        }
    }

    /**
     * Bitmap 데이터를 Byte Array 형태로 변환 (이미지 compression 포함)
     * @param bitmap(Bitmap): Bitmap 타입의 이미지 데이터
     * @param quality(Int): 이미지의 품질 (%) - default=100
     * @return ByteArray: 변환된 byte array
     * @author Seunggun Sin
     * @since 2022-07-09
     */
    private fun convertBitmapToByte(bitmap: Bitmap, quality: Int = 100): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            quality,
            byteArrayOutputStream
        ) // compression 및 변환
        return byteArrayOutputStream.toByteArray()
    }
}