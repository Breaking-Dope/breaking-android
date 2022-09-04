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
        const val MULTIPART_PROFILE_KEY = "profileImg" // multi part 프로필 요청에 대한 key 이름
        const val MULTIPART_POST_KEY = "mediaList"  // multi part 게시글 요청에 대한 key 이름
        const val JWT_HEADER_KEY = "authorization" // JWT 토큰 검증을 위한 헤더 키 값
        const val REFRESH_JWT_HEADER_KEY = "authorization-refresh" // refresh token 키 값
        const val JWT_REQUEST_PREFIX = "Bearer " // Jwt 토큰을 헤더에 넣고 요청할 때 필요한 접두사
        const val FILTER_DATE_FORMAT_SUFFIX = "T00:00" // 피드 요청 시 "기간" 필터의 날짜 형식에 대한 접미사
        const val FEED_SIZE = 15 // 피드 요청마다 가져올 게시글 개수
        const val FOLLOW_SIZE = 15 // 팔로우 리스트 요청 시 가져올 아이템 개수
        const val VIEW_TYPE_ITEM = 0 // 일반 아이템에 대한 레이아웃 view type
        const val VIEW_TYPE_LOADING = 1 // 로딩 아이템에 대한 레이아웃 view type
        const val USER_FEED_SIZE = 10 // 유저 페이지 피드 요청마다 가져올 게시글 개수
        const val TRANSACTION_SIZE = 25 // 입출금 내역 리스트 가져올 개수
        const val COMMENT_SIZE = 3 // 댓글 요청마다 가져올 댓글 개수
        const val NESTED_COMMENT_SIZE = 10 // 대댓글 요청마다 가져올 대댓글 개수

        val TAB_ICONS = arrayOf(
            R.drawable.ic_baseline_create_24,
            R.drawable.ic_baseline_add_shopping_cart_24,
            R.drawable.ic_baseline_bookmark_border_24
        ) // 게시글 아이콘 리스트

        val FINANCE_TAB_TEXT = arrayOf(
            "충전하기",
            "출금하기",
            "입출금 내역"
        )

        val FILTER_SELL_OPTIONS = arrayOf(
            "all",
            "sold",
            "unsold"
        ) // 피드 요청 시 필터 옵션 중 판매 상태에 대한 request parameter 리스트

        val SORT_OPTIONS = arrayOf(
            "chronological",
            "like",
            "view"
        ) // 피드 요청 시 정렬 옵션에 대한 request parameter 리스트

        val SORT_OPTIONS_VIEW = arrayOf(
            R.string.order_latest,
            R.string.order_like,
            R.string.order_view
        ) // 정렬 옵션 선택 시, view 에 보여지는 텍스트

        fun getDefaultProfile(context: Context): Bitmap =
            AppCompatResources.getDrawable(context, R.drawable.ic_default_profile_image)
                ?.toBitmap()!!

        fun getDefaultPost(context: Context): Bitmap =
            AppCompatResources.getDrawable(context, R.drawable.ic_default_post_image)
                ?.toBitmap()!!
    }
}
