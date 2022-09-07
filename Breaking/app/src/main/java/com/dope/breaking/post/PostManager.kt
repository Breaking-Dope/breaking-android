package com.dope.breaking.post

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.FollowData
import com.dope.breaking.model.request.RequestComment
import com.dope.breaking.model.request.RequestPostData
import com.dope.breaking.model.request.RequestPostDataModify
import com.dope.breaking.model.response.*
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.ValueUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.Throws

class PostManager {
    private val TAG = "PostManager.kt"

    /**
     * input 으로 받아온 제보 게시글 정보를 바탕으로 Request Body 생성 및 게시글 작성 요청하는 메소드
     * @param inputData(RequestPostData): 제보글 작성 필드에 대한 데이터 클래스 객체
     * @param imageData(Bitmap): 제보 미디어에 대한 bitmap 데이터
     * @param imageName(String): 제보 미디어의 파일 이름
     * @param fileList(ArrayList<File>) : 이미지/영상 파일
     * @param uriList(ArrayList<Uri>) : 이미지/영상 uri
     * @throws ResponseErrorException: 정상 응답 (2xx) 이외의 응답이 왔을 때 exception 발생
     * @return ResponsePostUpload: 응답 바디로 오는 postId를 담아주기 위한 클래스 타입
     * @author Tae hyun Park
     * @since 2022-08-02 | 2022-09-05
     */
    @Throws(ResponseErrorException::class)
    suspend fun startPostUpload(
        inputData: RequestPostData,
        imageData: ArrayList<Bitmap>, // 필요할 지 미지수
        imageName: ArrayList<String>,
        token: String,
        fileList: ArrayList<File>,
        uriList: ArrayList<Uri>
    ): ResponsePostUpload {
        // MultiPart.Body List 선언 및 초기화
        var multipartList = ArrayList<MultipartBody.Part>()

        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 받아온 미디어의 사이즈만큼 반복하여 multipart List 생성
        for (i in 0 until fileList.size) {
            if (fileList.size == 0) break // 받아온 미디어가 없으면 반복문 탈출
            // 이미지,영상에 대한 RequestBody 생성 (image/* video/*)
            val imageRequestBody = if (uriList[i].toString().contains("image")){ // 이미지면 bitmap
                RequestBody.create(
                    MediaType.parse("image/* video/*"),
                    convertBitmapToByte(imageData[i])
                )
            } else { // 영상이면 file type
                RequestBody.create(
                    MediaType.parse("image/* video/*"),
                    fileList[i]
                )
            }

            // 이미지에 대한 RequestBody 를 바탕으로 Multi form 데이터 리스트 생성
            multipartList.add(
                MultipartBody.Part.createFormData(
                    ValueUtil.MULTIPART_POST_KEY,
                    imageName[i],
                    imageRequestBody
                )
            )
        }
        // 나머지 필드에 대한 RequestBody 생성 (text/plain)
        val objectMapper = ObjectMapper()
        val result = objectMapper.writeValueAsString(inputData)

        val data =
            RequestBody.create(
                MediaType.parse("text/plain"),
                result
            ) // inputData.convertJsonToString()

        Log.d(TAG, "json 테스트1 : " + inputData.convertJsonToString())
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
     * 받아온 제보 postId 값을 바탕으로 게시글 상세 조회 정보를 받아오기 위한 요청을 하는 메소드
     * @param token(String): jwt token 값
     * @param postId(Long): 상세 조회 할 postId 값
     * @throws ResponseErrorException: 정상 응답 (2xx) 이외의 응답이 왔을 때 exception 발생
     * @return ResponsePostDetail: 응답 바디로 오는 게시글 상세 조회 DTO
     * @author Tae hyun Park
     * @since 2022-08-18
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetPostDetail(
        token: String,
        postId: Long
    ): ResponsePostDetail {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 게시글 상세 조회 요청
        val response = service.requestPostDetail(token, postId)

        if (response.code() in 200..299) { // 요청이 성공적이라면
            return response.body()!! // 응답의 response 객체 리턴
        } else { // 실패했다면
            // 예외 던지기
            throw ResponseErrorException("요청에 실패하였습니다. error: ${response.errorBody()?.string()}")
        }
    }

    /**
     * 게시물에 대해 댓글 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param postId(Long) : 댓글을 달고자 하는 현재 게시물 id
     * @param content(String) : 댓글 내용
     * @param hashTagList(ArrayList<String) : 해시태그 리스트 (옵션)
     * @author Tae hyun Park
     * @since 2022-09-01
     */
    @Throws(ResponseErrorException::class)
    suspend fun startRegisterComment(
        token: String,
        postId: Long,
        content: String,
        hashTagList : ArrayList<String>?
    ): Boolean {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 댓글 등록 요청
        val response = service.requestCommentWrite(
            token,
            postId,
            RequestComment(content, hashTagList)
        )

        // 요청이 성공적이라면
        if (response.isSuccessful) {
            Log.d(TAG,"요청 성공")
            return response.code() in 200..299
        } else {
            Log.d(TAG, "요청 실패 : ${response.errorBody()?.string()}")
            throw ResponseErrorException("요청에 실패하였습니다. error: ${response.errorBody()?.string()}")
        }
    }

    /**
     * 게시물에 대해 대댓글 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param commentId(Long) : 대댓글을 달고자 하는 현재 댓글 id
     * @param content(String) : 대댓글 내용
     * @param hashTagList(ArrayList<String) : 해시태그 리스트 (옵션)
     * @author Tae hyun Park
     * @since 2022-09-02
     */
    @Throws(ResponseErrorException::class)
    suspend fun startRegisterNestedComment(
        token: String,
        commentId: Long,
        content: String,
        hashTagList : ArrayList<String>?
    ): Boolean {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 대댓글 등록 요청
        val response = service.requestNestedCommentWrite(
            token,
            commentId,
            RequestComment(content, hashTagList)
        )

        // 요청이 성공적이라면
        if (response.isSuccessful) {
            Log.d(TAG,"요청 성공")
            return response.code() in 200..299
        } else {
            Log.d(TAG, "요청 실패 : ${response.errorBody()?.string()}")
            throw ResponseErrorException("요청에 실패하였습니다. error: ${response.errorBody()?.string()}")
        }
    }

    /**
     * 게시물의 댓글 리스트 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param postId(Long) : 게시물 id
     * @param lastCommentId(Int) : 마지막으로 요청한 댓글 id (최초 요청 시, 0 또는 null)
     * @param contentSize(Int) : 요청할 댓글 개수 (기본 3개)
     * @author Tae hyun Park
     * @since 2022-09-01
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetCommentList(
        token: String,
        postId: Long,
        lastCommentId: Int,
        contentSize: Int,
    ): List<ResponseComment> {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 댓글 리스트 요청
        val resultList = service.requestCommentList(
            token,
            postId,
            lastCommentId,
            contentSize
        )

        if (resultList.code() in 200..299) { // 요청에 성공했다면
            return resultList.body()!! // 응답 리스트 리턴
        } else { // 실패했다면
            throw ResponseErrorException("${resultList.errorBody()?.string()}") // 예외 발생
        }
    }

    /**
     * 게시물의 대댓글 리스트 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param commentId(Long) : 대댓글 리스트를 요청할 댓글 id
     * @param lastCommentId(Int) : 마지막으로 요청한 대댓글 id (최초 요청 시, 0 또는 null)
     * @param contentSize(Int) : 요청할 대댓글 개수 (기본 5개)
     * @author Tae hyun Park
     * @since 2022-09-02
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetNestedCommentList(
        token: String,
        commentId: Long,
        lastCommentId: Int,
        contentSize: Int,
    ): List<ResponseComment> {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 대댓글 리스트 요청
        val resultList = service.requestNestedCommentList(
            token,
            commentId,
            lastCommentId,
            contentSize
        )

        if (resultList.code() in 200..299) { // 요청에 성공했다면
            return resultList.body()!! // 응답 리스트 리턴
        } else { // 실패했다면
            throw ResponseErrorException("${resultList.errorBody()?.string()}") // 예외 발생
        }
    }

    /**
     * 게시물 수정 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param postId(Long) : 수정할 게시글 id
     * @param postInfo(RequestPostData) : 수정된 게시글 정보 dto
     * @author Tae hyun Park
     * @since 2022-09-04
     */
    @Throws(ResponseErrorException::class)
    suspend fun startEditPost(
        token: String,
        postId: Long,
        postInfo : RequestPostDataModify
    ): ResponsePostUpload {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 게시글 수정 요청
        val resultList = service.requestPostDetailEdit(
            token,
            postId,
            postInfo
        )

        if (resultList.code() in 200..299) { // 요청에 성공했다면
            return resultList.body()!! // 응답 리스트 리턴
        } else { // 실패했다면
            throw ResponseErrorException("${resultList.errorBody()?.string()}") // 예외 발생
        }
    }

    /**
     * 게시물 삭제 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param postId(Long) : 삭제할 게시글 id
     * @author Tae hyun Park
     * @since 2022-09-05
     */
    @Throws(ResponseErrorException::class)
    suspend fun startDeletePost(
        token: String,
        postId: Long,
    ): Boolean {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 게시글 삭제 요청
        val response = service.requestPostDetailDelete(
            token,
            postId,
        )

        // 요청이 성공적이라면
        if (response.isSuccessful) {
            Log.d(TAG,"요청 성공")
            return response.code() in 200..299
        } else {
            Log.d(TAG, "요청 실패 : ${response.errorBody()?.string()}")
            throw ResponseErrorException("요청에 실패하였습니다. error: ${response.errorBody()?.string()}")
        }
    }

    /**
     * 게시물에 좋아요 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param postId(Long) : 좋아요 하고자 하는 게시물 id
     * @author Tae hyun Park
     * @since 2022-09-06
     */
    @Throws(ResponseErrorException::class)
    suspend fun startPostLike(
        token: String,
        postId: Long,
    ): Boolean {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 게시글 좋아요 요청
        val response = service.requestPostLike(
            token,
            postId,
        )

        // 요청이 성공적이라면
        if (response.isSuccessful) {
            Log.d(TAG,"요청 성공")
            return response.code() in 200..299
        } else {
            var errorString = response.errorBody()?.string()!!
            var jsonObject: JsonObject =
                JsonParser.parseString(errorString).asJsonObject
            if(jsonObject.get("code").toString().replace("\"", "") == "BSE458"){
                throw ResponseErrorException("BSE458")
            }else{
                throw ResponseErrorException(errorString)
            }
        }
    }

    /**
     * 게시물에 좋아요 취소 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param postId(Long) : 좋아요 취소하고자 하는 게시물 id
     * @author Tae hyun Park
     * @since 2022-09-06
     */
    @Throws(ResponseErrorException::class)
    suspend fun startCancelPostLike(
        token: String,
        postId: Long,
    ): Boolean {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 게시글 좋아요 취소 요청
        val response = service.requestCancelPostLike(
            token,
            postId,
        )

        // 요청이 성공적이라면
        if (response.isSuccessful) {
            Log.d(TAG,"요청 성공")
            return response.code() in 200..299
        } else {
            var errorString = response.errorBody()?.string()!!
            var jsonObject: JsonObject =
                JsonParser.parseString(errorString).asJsonObject
            if(jsonObject.get("code").toString().replace("\"", "") == "BSE459"){
                throw ResponseErrorException("BSE459")
            }else{
                throw ResponseErrorException(errorString)
            }
        }
    }

    /**
     * 게시물의 좋아요 리스트 요청을 보내기 위한 메소드
     * @param token(String) : jwt token 값
     * @param postId(Long) : 좋아요 리스트를 요청할 게시물 id
     * @param lastUserId(Int) : 마지막으로 요청한 유저 id (최초 요청 시, 0 또는 null)
     * @param contentSize(Int) : 요청할 좋아요 리스트 개수 (기본 5개)
     * @author Tae hyun Park
     * @since 2022-09-06
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetPostLikeList(
        token: String,
        postId: Long,
        lastUserId: Int,
        contentSize: Int,
    ): List<FollowData> {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 게시글 좋아요 리스트 요청
        val resultList = service.requestPostLikeList(
            token,
            postId,
            lastUserId,
            contentSize
        )

        if (resultList.code() in 200..299) { // 요청에 성공했다면
            return resultList.body()!! // 응답 리스트 리턴
        } else { // 실패했다면
            throw ResponseErrorException("${resultList.errorBody()?.string()}") // 예외 발생
        }
    }

    /**
     * 메인 피드 요청을 통해 리스트를 가져옴 (필터 & 정렬 옵션 포함)
     * @param lastPostId(Int): 마지막으로 요청한 마지막 게시글 id (최초 요청 시, 0 또는 null)
     * @param contentSize(Int): 요청할 게시글 개수(현재 10개)
     * @param sortIndex(Int): 정렬 옵션에서 선택한 라디오 버튼 인덱스
     * @param sellIndex(Int): 필터 옵션에서 판매 제보의 라디오 버튼 인덱스
     * @param startDate(String): 시작 날짜 (yyyy-MM-dd)
     * @param endDate(String): 종료 날짜 (yyyy-MM-dd)
     * @param lastMin(Int): 최근 N분에서 입력한 N 값
     * @param token(String): 본인의 Jwt 토큰
     * @return List<ResponseMainFeed>: 게시글 데이터 리스트
     * @throws ResponseErrorException: 요청 에러 시 발생
     * @author Seunggun Sin
     * @since 2022-08-15 | 2022-08-30
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetMainFeed(
        lastPostId: Int,
        contentSize: Int,
        sortIndex: Int,
        sellIndex: Int,
        startDate: String,
        endDate: String,
        lastMin: Int,
        token: String
    ): List<ResponseMainFeed> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java) // retrofit 객체 생성

        val resultList = service.requestGetMainFeed(
            token,
            lastPostId,
            contentSize,
            null,
            null,
            ValueUtil.SORT_OPTIONS[sortIndex],
            ValueUtil.FILTER_SELL_OPTIONS[sellIndex],
            if (startDate == "-") null else startDate + ValueUtil.FILTER_DATE_FORMAT_SUFFIX,
            if (endDate == "-") null else endDate + ValueUtil.FILTER_DATE_FORMAT_SUFFIX,
            if (lastMin == -1) null else lastMin
        ) // 게시글 요청해서 받아오기

        if (resultList.code() in 200..299) { // 요청에 성공했다면
            return resultList.body()!! // 응답 리스트 리턴
        } else { // 실패했다면
            throw ResponseErrorException("${resultList.errorBody()?.string()}") // 예외 발생
        }
    }

    /**
     * 메인 피드 요청을 통해 리스트를 가져옴 (필터 & 정렬 옵션 포함)
     * @param userId(Int): 대상 유저 id
     * @param cursorId(Int): 마지막으로 요청한 리스트에서 마지막 아이템의 게시글 id
     * @param contentSize(Int): 요청할 게시글 개수(현재 7개)
     * @param option(Int): 피드 구분 옵션
     * @param soldOption(String): 판매 상태 옵션
     * @param token(String): 본인의 Jwt 토큰
     * @return List<ResponseMainFeed>: 게시글 데이터 리스트
     * @throws ResponseErrorException: 요청 에러 시 발생
     * @author Seunggun Sin
     * @since 2022-08-19
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetUserPageFeed(
        userId: Long,
        cursorId: Int,
        contentSize: Int,
        option: String,
        soldOption: String,
        token: String
    ): List<ResponseMainFeed> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        val resultList =
            service.requestGetUserPageFeed(token, userId, option, cursorId, contentSize, soldOption)
        if (resultList.code() in 200..299) {
            return resultList.body()!!
        } else {
            throw ResponseErrorException("${resultList.errorBody()?.string()}")
        }
    }

    /**
     * 문자열 검색을 통한 피드 리스트 불러오는 요청 (필터 & 정렬 옵션 포함)
     * @param cursorId(Int): 마지막으로 요청한 리스트에서 마지막 아이템의 게시글 id
     * @param contentSize(Int): 요청할 게시글 개수(현재 7개)
     * @param searchContent(String): 문자열 검색을 하고자하는 검색 키워드
     * @param sortIndex(Int): 정렬 옵션에서 선택한 라디오 버튼 인덱스
     * @param sellIndex(Int): 필터 옵션에서 판매 제보의 라디오 버튼 인덱스
     * @param startDate(String): 시작 날짜 (yyyy-MM-dd)
     * @param endDate(String): 종료 날짜 (yyyy-MM-dd)
     * @param lastMin(Int): 최근 N분에서 입력한 N 값
     * @param token(String): 본인의 Jwt 토큰
     * @return List<ResponseMainFeed>: 게시글 데이터 리스트
     * @throws ResponseErrorException: 요청 에러 시 발생
     * @author Seunggun Sin
     * @since 2022-08-30
     */
    @Throws(ResponseErrorException::class)
    suspend fun startSearchStringFeed(
        cursorId: Int,
        contentSize: Int,
        searchContent: String,
        sortIndex: Int,
        sellIndex: Int,
        startDate: String,
        endDate: String,
        lastMin: Int,
        token: String
    ): List<ResponseMainFeed> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 검색 키워드에서 공백은 +로 대체
        val replaceSpace = searchContent.replace(" ", "+")

        val response = service.requestGetMainFeed(
            token,
            cursorId,
            contentSize,
            replaceSpace,
            null,
            ValueUtil.SORT_OPTIONS[sortIndex],
            ValueUtil.FILTER_SELL_OPTIONS[sellIndex],
            if (startDate == "-") null else startDate + ValueUtil.FILTER_DATE_FORMAT_SUFFIX,
            if (endDate == "-") null else endDate + ValueUtil.FILTER_DATE_FORMAT_SUFFIX,
            if (lastMin == -1) null else lastMin
        ) // 검색 결과에 대한 요청

        if (response.code() in 200..299)
            return response.body()!!
        else
            throw ResponseErrorException("${response.errorBody()?.string()}")
    }

    /**
     * 해시태그 검색을 통한 피드 리스트 불러오는 요청 (필터 & 정렬 옵션 포함)
     * @param cursorId(Int): 마지막으로 요청한 리스트에서 마지막 아이템의 게시글 id
     * @param contentSize(Int): 요청할 게시글 개수(현재 7개)
     * @param hashtagContent(String): 해시태그 검색을 하고자하는 검색 키워드
     * @param sortIndex(Int): 정렬 옵션에서 선택한 라디오 버튼 인덱스
     * @param sellIndex(Int): 필터 옵션에서 판매 제보의 라디오 버튼 인덱스
     * @param startDate(String): 시작 날짜 (yyyy-MM-dd)
     * @param endDate(String): 종료 날짜 (yyyy-MM-dd)
     * @param lastMin(Int): 최근 N분에서 입력한 N 값
     * @param token(String): 본인의 Jwt 토큰
     * @return List<ResponseMainFeed>: 게시글 데이터 리스트
     * @throws ResponseErrorException: 요청 에러 시 발생
     * @author Seunggun Sin
     * @since 2022-08-30
     */
    @Throws(ResponseErrorException::class)
    suspend fun startSearchHashtagFeed(
        cursorId: Int,
        contentSize: Int,
        hashtagContent: String,
        sortIndex: Int,
        sellIndex: Int,
        startDate: String,
        endDate: String,
        lastMin: Int,
        token: String
    ): List<ResponseMainFeed> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        // 공백과 # 제거
        val replaceSpace = hashtagContent.replace(" ", "").replace("#", "")

        val response = service.requestGetMainFeed(
            token,
            cursorId,
            contentSize,
            null,
            replaceSpace,
            ValueUtil.SORT_OPTIONS[sortIndex],
            ValueUtil.FILTER_SELL_OPTIONS[sellIndex],
            if (startDate == "-") null else startDate + ValueUtil.FILTER_DATE_FORMAT_SUFFIX,
            if (endDate == "-") null else endDate + ValueUtil.FILTER_DATE_FORMAT_SUFFIX,
            if (lastMin == -1) null else lastMin
        ) // 검색 요청에 대한 결과 받기

        if (response.code() in 200..299)
            return response.body()!!
        else
            throw ResponseErrorException("${response.errorBody()?.string()}")
    }

    /**
     * postId 에 해당하는 게시글에 북마크를 등록하는 요청 (토큰 필수)
     * @param postId(Int): 등록하고자 하는 게시글 id
     * @param token(String): 본인의 Jwt 토큰
     * @return Boolean: 북마크 등록 성공 시 true, 실패 시 false
     * @author Seunggun Sin
     * @since 2022-08-19
     */
    suspend fun startRegisterBookmark(postId: Int, token: String): Boolean {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        val response = service.requestBookmark(token, postId)
        return response.code() in 200..299
    }

    /**
     * postId 에 해당하는 게시글에 북마크를 해제하는 요청 (토큰 필수)
     * @param postId(Int): 해제하고자 하는 게시글 id
     * @param token(String): 본인의 Jwt 토큰
     * @return Boolean: 북마크 해제 성공 시 true, 실패 시 false
     * @author Seunggun Sin
     * @since 2022-08-19
     */
    suspend fun startUnRegisterBookmark(postId: Int, token: String): Boolean {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        val response = service.requestUnBookmark(token, postId)
        return response.code() in 200..299
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