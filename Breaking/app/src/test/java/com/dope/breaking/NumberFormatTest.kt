package com.dope.breaking

import com.dope.breaking.util.NumberUtil
import org.junit.Test

class NumberFormatTest {
    @Test
    fun `숫자 카운트 축약 포맷 테스트`() {
        val number1 = 9999
        val number2 = 526
        val number3 = 24500
        val number4 = 6452100
        val number5 = 999999999
        assert("9.9천" == NumberUtil().countNumberFormatter(number1))
        assert("526" == NumberUtil().countNumberFormatter(number2))
        assert("2.4만" == NumberUtil().countNumberFormatter(number3))
        assert("645.2만" == NumberUtil().countNumberFormatter(number4))
        assert("9.9억" == NumberUtil().countNumberFormatter(number5))
    }
}