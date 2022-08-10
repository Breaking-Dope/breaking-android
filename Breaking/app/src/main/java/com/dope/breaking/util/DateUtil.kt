package com.dope.breaking.util

import java.text.SimpleDateFormat
import java.util.*

class DateUtil {

    /**
     * 현재 날짜와 가져온 날짜의 차이를 적절한 범위에 해당하는 문자열을 반환하는 함수
     * @param date(String): 문자열 형태의 날짜 데이터 (포맷: "yyyy-MM-ddTHH:mm:ss.fp" or "yyyy-MM-dd HH:mm:ss"")
     * @return String: 현재와 날짜 차이를 적절한 범위에 해당하는 초/분/시/일 단위로 반환
     * @author Seunggun Sin
     * @since 2022-08-10
     */
    fun getTimeDiff(date: String): String {
        if (!(date.length == 26 || date.length == 19)) { // 형식에 맞지 않으면 에러 리턴
            return "-"
        }
        val today = Calendar.getInstance() // 오늘 날짜 가져오기
        var newDate: String = date // 가져온 날짜를 가공할 변수

        if (date.length == 26) { // 서버 기준 포맷이라면
            newDate = date.replace("T", " ")
            newDate = newDate.split(".")[0]
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") // 형식 맞추기
            val targetFormat = dateFormat.parse(newDate) // 기준 날짜 형식에 맞추기

            var calcValue = (today.time.time - targetFormat.time) / 1000 // 초 단위로 날짜 차이 계산
            /*
                날짜 차이를 나누는 기준
                / 1000: 초 단위
                / 60 * 1000: 분 단위
                / 60 * 60 * 1000: 시 단위
                / 60 * 60 * 24 * 1000: 일 단위
             */
            var suffix: String = "초"
            if (calcValue < 60) { // 60초 이내라면
                suffix = "초" // 초 단위 사용
            } else {
                calcValue /= 60
                if (calcValue < 60) { // 60분 이내라면
                    suffix = "분" // 분 단위 사용
                } else {
                    calcValue /= 60
                    if (calcValue < 24) { // 24시간 이내라면
                        suffix = "시간" // 시 단위 사용
                    } else {
                        calcValue /= 24
                        suffix = "일" // 일 단위 사용
                    }
                }
            }
            return "$calcValue$suffix 전" // 최종 가공한 날짜 차이 리턴
        } catch (e: Exception) {
            return "-" // 에러 값 리턴
        }
    }
}