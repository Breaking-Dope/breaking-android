package com.dope.breaking

import com.dope.breaking.model.RequestSignUp
import org.junit.Test

class RequestSignUpTest {
    @Test
    fun `클래스에서 if문 초기화가 바로 적용 되는지 테스트`() {
        val result = RequestSignUp("a", "b", "c", "d", "e", "f", true)
        assert(result.role == "USER")
    }

}