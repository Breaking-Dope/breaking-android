package com.dope.breaking.util

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.dope.breaking.R

/**
 * 여러군데에서 사용되는 hard coded 값들을 저장하는 클래스
 */
class ValueUtil {
    companion object {
        const val IMAGE_BASE_URL = "https://team-dope.link:8443" // 이미지 호출을 위한 서버 base url
        const val MULTIPART_PROFILE_KEY = "profileImg" // multi part 요청에 대한 key 이름
        const val JWT_HEADER_KEY = "authorization" // JWT 토큰 검증을 위한 헤더 키 값
        const val JWT_REQUEST_PREFIX = "Bearer " // Jwt 토큰을 헤더에 넣고 요청할 때 필요한 접두사

        val TAB_ICONS = arrayOf(
            R.drawable.ic_baseline_create_24,
            R.drawable.ic_baseline_add_shopping_cart_24,
            R.drawable.ic_baseline_bookmark_border_24
        ) // 게시글 아이콘 리스트

        fun getDefaultProfile(context: Context): Bitmap =
            AppCompatResources.getDrawable(context, R.drawable.ic_default_profile_image)
                ?.toBitmap()!!
    }
}
