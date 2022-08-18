package com.dope.breaking.util

import java.math.RoundingMode
import java.text.DecimalFormat

class NumberUtil {
    /**
     * 좋아요, 댓글 수 등 숫자 카운트에 대해 긴 숫자들을 축약하도록 하는 함수
     * 소수점 한자리까지만 허용하여 범위의 최소 값으로 나눈 뒤, 단위를 붙임
     * @param number(Int): 변환할 숫자
     * @return String: 받아온 숫자를 축약하여 표현한 문자열
     * @author Seunggun Sin
     * @since 2022-08-16
     */
    fun countNumberFormatter(number: Int): String {
        val decimalFormat = DecimalFormat("#.#") // 소수점 한자리까지만 허용
        decimalFormat.roundingMode = RoundingMode.DOWN // 소수점 내림 모드
        return when (number) {
            in 0..999 -> number.toString() // 1000 이하는 그대로 출력
            in 1000..9999 -> decimalFormat.format(number / 1000.0).toString() // 천단위
                .replace(".0", "") + "천"
            in 10000..99999999 -> decimalFormat.format(number / 10000.0).toString() // 만단위
                .replace(".0", "") + "만"
            in 100000000..2147483647 -> decimalFormat.format(number / 100000000.0).toString() // 억단위
                .replace(".0", "") + "억"
            else -> "21억 이상" // 음수는 없다고 가정하고, Int 최대 범위 이상 넘어갈 시 처리(어차피 안들어옴)
        }
    }
}