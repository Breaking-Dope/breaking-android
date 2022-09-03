package com.dope.breaking.finance

import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.request.RequestAmount
import com.dope.breaking.model.response.ResponseTransaction
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import kotlin.jvm.Throws

class FinanceManager {

    /**
     * amount 만큼 입금을 요청하는 함수
     * @param token(String): 본인의 Jwt 토큰 (필수)
     * @param amount(Int): 입금하고자 하는 양
     * @return Boolean: 입금의 성공 여부
     * @author Seunggun Sin
     * @since 2022-09-03
     */
    suspend fun startDepositRequest(
        token: String,
        amount: Int
    ): Boolean {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        val response = service.requestDeposit(token, RequestAmount(amount))
        return response.code() in 200..299
    }

    /**
     * amount 만큼 출금을 요청하는 함수
     * @param token(String): 본인의 Jwt 토큰 (필수)
     * @param amount(Int): 출금하고자 하는 양
     * @return Boolean: 출금의 성공 여부
     * @author Seunggun Sin
     * @since 2022-09-03
     */
    suspend fun startWithdrawRequest(
        token: String,
        amount: Int
    ): Boolean {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        val response = service.requestWithdraw(token, RequestAmount(amount))
        return response.code() in 200..299
    }

    /**
     * 본인의 입출금 내역 리스트를 불러오는 요청
     * @param token(String): 본인의 Jwt 토큰
     * @param cursor(Int): 마지막으로 요청한 리스트의 마지막 인덱스
     * @param size(Int): 가져올 아이템 개수
     * @return List<ResponseTransaction>: 입출금 내역 리스트
     * @throws ResponseErrorException: 응답 에러
     * @author Seunggun Sin
     * @since 2022-09-02
     */
    @Throws(ResponseErrorException::class)
    suspend fun startGetFinanceList(
        token: String,
        cursor: Int,
        size: Int
    ): List<ResponseTransaction> {
        val service = RetrofitManager.retrofit.create(RetrofitService::class.java)

        val response = service.requestTransactionList(token, cursor, size)

        if (response.code() in 200..299)
            return response.body()!!
        else
            throw ResponseErrorException("${response.errorBody()?.string()}")
    }
}