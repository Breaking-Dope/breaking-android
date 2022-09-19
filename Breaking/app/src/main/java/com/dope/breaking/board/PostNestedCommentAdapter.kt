package com.dope.breaking.board

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dope.breaking.R
import com.dope.breaking.databinding.ActivityPostDetailBinding
import com.dope.breaking.databinding.CustomCommentMorePopupBinding
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.request.RequestComment
import com.dope.breaking.model.response.ResponseComment
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.post.PostManager
import com.dope.breaking.user.UserProfile
import com.dope.breaking.util.DateUtil
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.Utils
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostNestedCommentAdapter(
    private val context: Context,
    var token: String,
    var data: MutableList<ResponseComment?>,
    var commentId: Long,
    var binding: ActivityPostDetailBinding
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ValueUtil.VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_nested_comment_list, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(data[position]!!, position)
            holder.commentProfile.setOnClickListener { // 답글 프로필 클릭 시 이동
                UserProfile(context as Activity).moveToUserPage(data[position]!!.user.userId)
            }
            holder.commentNickname.setOnClickListener { // 답글 닉네임 클릭 시 이동
                UserProfile(context as Activity).moveToUserPage(data[position]!!.user.userId)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commentProfile = itemView.findViewById<ImageView>(R.id.iv_comment_profile_nested)
        val commentNickname = itemView.findViewById<TextView>(R.id.tv_comment_nickname_nested)
        private val commentContent = itemView.findViewById<TextView>(R.id.tv_comment_content_nested)
        private val commentButtonLike = itemView.findViewById<ImageButton>(R.id.ib_post_like_nested)
        private val commentCountLike =
            itemView.findViewById<TextView>(R.id.tv_post_like_count_nested)
        private val commentDate = itemView.findViewById<TextView>(R.id.tv_post_time_nested)

        private val moreMenu = itemView.findViewById<ImageButton>(R.id.ib_post_more_nested)
        private val commentInput = itemView.findViewById<EditText>(R.id.et_comment_content_nested)
        private val commentCancel = itemView.findViewById<TextView>(R.id.tv_cancel_nested)
        private val commentRegister = itemView.findViewById<TextView>(R.id.tv_register_nested)
        private val constraintSet =
            itemView.findViewById<ConstraintLayout>(R.id.constraint_view_nested)

        fun bind(item: ResponseComment, position: Int) {
            var isContentButtonPressed = false // 댓글 본문이 눌렸는지 안 눌렸는지

            if (item.user.profileImgUrl == null) {
                Glide.with(itemView)
                    .load(R.drawable.ic_default_profile_image)
                    .circleCrop()
                    .fitCenter()
                    .into(commentProfile)
            } else {
                Glide.with(itemView)
                    .load(ValueUtil.IMAGE_BASE_URL + item.user.profileImgUrl)
                    .placeholder(R.drawable.ic_default_profile_image)
                    .error(R.drawable.ic_default_profile_image)
                    .circleCrop()
                    .into(commentProfile)
            }

            val popupInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupBind =
                CustomCommentMorePopupBinding.inflate(popupInflater) // 커스텀 팝업 레이아웃 binding inflate

            val popupWindow = PopupWindow(
                popupBind.root,
                ViewGroup.LayoutParams.WRAP_CONTENT, // 가로 길이
                ViewGroup.LayoutParams.WRAP_CONTENT, // 세로 길이
                true
            ) // 팝업 윈도우 화면 설정

            moreMenu.setOnClickListener(popupWindow::showAsDropDown) // 더보기 메뉴 클릭 시, 메뉴 view 중심으로 팝업 메뉴 호출

            if (item.user.userId == ResponseExistLogin.baseUserInfo?.userId) { // 내 대댓글이면 수정, 삭제 메뉴가 보이게
                popupBind.layoutHorizEditComment.visibility = View.VISIBLE
                popupBind.layoutHorizDeleteComment.visibility = View.VISIBLE
                popupBind.layoutHorizChatComment.visibility = View.GONE
                popupBind.layoutHorizBanComment.visibility = View.GONE
            } else { // 타 유저 대댓글이면 채팅, 차단 메뉴가 보이게
                popupBind.layoutHorizEditComment.visibility = View.GONE
                popupBind.layoutHorizDeleteComment.visibility = View.GONE
                popupBind.layoutHorizChatComment.visibility = View.VISIBLE
                popupBind.layoutHorizBanComment.visibility = View.VISIBLE
            }

            // 수정 뷰에서 취소 버튼 누르면 기존 뷰 원상복구
            commentCancel.setOnClickListener {
                changeViewCommentModifyBefore() // 기존 뷰로 폼 변경
            }

            // 수정 메뉴 클릭 시
            popupBind.layoutHorizEditComment.setOnClickListener {
                changeViewCommentModifyAfter(popupWindow) // 수정 뷰로 폼 변경
            }

            // 삭제 메뉴 클릭 시
            popupBind.layoutHorizDeleteComment.setOnClickListener {
                popupWindow.dismiss()
                // 삭제 다이얼로그 띄우고 요청
                DialogUtil().MultipleDialog(
                    context,
                    "답글을 삭제하시겠습니까?",
                    "예",
                    "아니오",
                    {
                        // 답글 삭제 요청 함수 시작
                        processPostCommentDelete(
                            token,
                            item.commentId.toLong(), {
                                if (it) {
                                    Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    // 대댓글 갱신
                                    processGetNestedCommentList(
                                        token,
                                        commentId,
                                        0, {
                                        }, {
                                            binding.tvPostCommentCount.text =
                                                (binding.tvPostCommentCount.text.toString()
                                                    .toInt() - data.size + it.size).toString()
                                            replaceAll(it)
                                        }, {
                                            it.printStackTrace()
                                            DialogUtil().SingleDialog(
                                                context,
                                                "답글 갱신에 문제가 발생하였습니다.",
                                                "확인"
                                            )
                                        }
                                    )
                                }
                            }, {
                                it.printStackTrace()
                                DialogUtil().SingleDialog(context, "댓글 삭제에 문제가 발생하였습니다.", "확인")
                            }
                        )
                    },
                    { popupWindow.dismiss() }).show()
            }

            // 채팅 메뉴 클릭 시
            popupBind.layoutHorizChatComment.setOnClickListener {
                popupWindow.dismiss() // 기능 미구현
            }

            // 차단 메뉴 클릭 시
            popupBind.layoutHorizBanComment.setOnClickListener {
                popupWindow.dismiss() // 기능 미구현
            }

            // 등록 버튼 누르면 수정 요청
            commentRegister.setOnClickListener {
                if (commentInput.text.toString().isNotEmpty()) {
                    processPostCommentEdit(
                        token,
                        item.commentId.toLong(),
                        RequestComment(
                            commentInput.text.toString(),
                            Utils.getArrayHashTagWithOutSpace(commentInput.text.toString())
                        ), { it ->
                            if (it) {
                                Toast.makeText(context, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                changeViewCommentModifyBefore() // 원래 댓글 화면으로 뷰 변화 주기
                                // 대댓글 갱신
                                processGetNestedCommentList(
                                    token,
                                    commentId,
                                    0, {
                                    }, {
                                        binding.tvPostCommentCount.text =
                                            (binding.tvPostCommentCount.text.toString()
                                                .toInt() - data.size + it.size).toString()
                                        replaceAll(it)
                                    }, {
                                        it.printStackTrace()
                                        DialogUtil().SingleDialog(
                                            context,
                                            "답글 갱신에 문제가 발생하였습니다.",
                                            "확인"
                                        )
                                    }
                                )
                            }
                        }, {
                            it.printStackTrace()
                            DialogUtil().SingleDialog(context, "답글 수정 요청에 문제가 발생하였습니다.", "확인")
                        }
                    )
                } else {
                    Toast.makeText(context, "수정할 내용을 입력해주세요!", Toast.LENGTH_SHORT).show()
                }
            }

            commentNickname.text = item.user.nickname
            commentContent.text = item.content
            if (item.isLiked) // 내가 좋아요가 누른 상태면
                commentButtonLike.background =
                    ContextCompat.getDrawable(context, R.drawable.ic_post_like_after)
            else // 아니라면
                commentButtonLike.background =
                    ContextCompat.getDrawable(context, R.drawable.ic_post_like)
            commentCountLike.text = item.likeCount.toString()
            commentDate.text = DateUtil().getTimeDiff(item.createdDate)
            setViewHashTag(commentContent, commentContent.text.toString()) // 대댓글 해시태그 강조

            /* 대댓글 좋아요 구현  */
            commentButtonLike.setOnClickListener {
                if (!item.isLiked) { // 좋아요가 안 눌렸다면 좋아요 요청
                    processPostCommentLike(
                        token,
                        item.commentId.toLong(), {
                            if (it) {
                                Log.d("PostNestedCommentAdapter.kt", "좋아요 성공")
                                commentButtonLike.background = ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_post_like_after
                                )
                                commentCountLike.text = (item.likeCount + 1).toString()
                                item.isLiked = true
                                item.likeCount = item.likeCount + 1
                            }
                        }, {
                            it.printStackTrace()
                            DialogUtil().SingleDialog(context, "좋아요 요청에 문제가 발생하였습니다.", "확인")
                        }
                    )
                } else { // 좋아요가 눌렸다면 좋아요 취소 요청
                    processPostCommentUnLike(
                        token,
                        item.commentId.toLong(), {
                            if (it) {
                                Log.d("PostNestedCommentAdapter.kt", "좋아요 취소 성공")
                                commentButtonLike.background =
                                    ContextCompat.getDrawable(context, R.drawable.ic_post_like)
                                commentCountLike.text = (item.likeCount - 1).toString()
                                item.isLiked = false
                                item.likeCount = item.likeCount - 1
                            }
                        }, {
                            it.printStackTrace()
                            DialogUtil().SingleDialog(context, "좋아요 취소 요청에 문제가 발생하였습니다.", "확인")
                        }
                    )
                }
            }

            /* 더보기 메뉴 구현 */
            commentContent.setOnClickListener { // 클릭 시
                if (!isContentButtonPressed) {
                    commentContent.maxLines = 10 // 다 보이게 (10줄로 기본 설정)
                    isContentButtonPressed = true
                } else {
                    commentContent.maxLines = 2 // 다시 2줄로 설정
                    isContentButtonPressed = false
                }
            }
        }

        /**
         * @description - 댓글/대댓글 수정 시에 뷰를 바꾸는 메소드 (Before)
         * @return - None
         * @author - Tae hyun Park
         * @since - 2022-09-10
         */
        private fun changeViewCommentModifyBefore() {
            // 기존 뷰 VISIBLE
            commentNickname.visibility = View.VISIBLE
            commentContent.visibility = View.VISIBLE
            commentButtonLike.visibility = View.VISIBLE
            commentCountLike.visibility = View.VISIBLE
            commentDate.visibility = View.VISIBLE
            moreMenu.visibility = View.VISIBLE
            // 수정 뷰 GONE
            commentInput.visibility = View.GONE
            commentCancel.visibility = View.GONE
            commentRegister.visibility = View.GONE
        }

        /**
         * @description - 댓글/대댓글 수정 시에 뷰를 바꾸는 메소드 (After)
         * @return - None
         * @author - Tae hyun Park
         * @since - 2022-09-10
         */
        private fun changeViewCommentModifyAfter(popupWindow: PopupWindow) {
            // 수정 뷰를 띄우기 위해 기존 뷰 GONE
            commentNickname.visibility = View.GONE
            commentContent.visibility = View.GONE
            commentButtonLike.visibility = View.GONE
            commentCountLike.visibility = View.GONE
            commentDate.visibility = View.GONE
            moreMenu.visibility = View.GONE
            // 수정 뷰 VISIBLE
            commentInput.visibility = View.VISIBLE
            commentCancel.visibility = View.VISIBLE
            commentRegister.visibility = View.VISIBLE
            popupWindow.dismiss()
            // 수정 전 기존 내용 그대로 보여주기
            commentInput.setText(commentContent.text.toString())
        }
    }

    inner class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val progressDialog = itemView.findViewById<ProgressBar>(R.id.progressbar_loading)
    }

    /**
     * @description - 게시물의 대댓글 리스트 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @param - commentId(Long) : 대댓글 리스트를 요청할 게시물 id
     * @param - lastCommentId(Int) : 가장 최근에 요청한 대댓글 id
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-02
     */
    private fun processGetNestedCommentList(
        token: String,
        commentId: Long,
        lastCommentId: Int,
        init: () -> Unit,
        last: (List<ResponseComment>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            init() // 초기화 함수 호출
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startGetNestedCommentList(
                    token,
                    commentId,
                    lastCommentId,
                    ValueUtil.NESTED_COMMENT_SIZE,
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
                Log.d("PostCommentAdapter.kt", "대댓글 리스트 요청 결과 : $response")
            } catch (e: ResponseErrorException) {
                error(e)
            }
        }
    }

    /**
     * 해당 제보 게시물의 특정 댓글/대댓글 좋아요를 요청하는 메소드
     * @param - token(String) : jwt 토큰
     * @param - commentId(Long) : 좋아요 요청할 댓글/대댓글 id
     * @author - Tae hyun Park
     * @since - 2022-09-10
     */
    private fun processPostCommentLike(
        token: String,
        commentId: Long,
        last: (Boolean) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startPostCommentLike(
                    token,
                    commentId
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e)
            }
        }
    }

    /**
     * 해당 제보 게시물의 특정 댓글/대댓글 좋아요 취소를 요청하는 메소드
     * @param - token(String) : jwt 토큰
     * @param - commentId(Long) : 좋아요 취소 요청할 댓글/대댓글 id
     * @author - Tae hyun Park
     * @since - 2022-09-10
     */
    private fun processPostCommentUnLike(
        token: String,
        commentId: Long,
        last: (Boolean) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startPostCommentUnLike(
                    token,
                    commentId
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e)
            }
        }
    }

    /**
     * 해당 제보 게시물의 특정 댓글/대댓글 수정을 요청하는 메소드
     * @param - token(String) : jwt 토큰
     * @param - commentId(Long) : 수정 요청할 댓글/대댓글 id
     * @param - commentInfo(RequestComment) : 수정할 댓글 dto
     * @author - Tae hyun Park
     * @since - 2022-09-10
     */
    private fun processPostCommentEdit(
        token: String,
        commentId: Long,
        commentInfo: RequestComment,
        last: (Boolean) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startPostCommentEdit(
                    token,
                    commentId,
                    commentInfo
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e)
            }
        }
    }

    /**
     * 해당 제보 게시물의 특정 댓글/대댓글 삭제를 요청하는 메소드
     * @param - token(String) : jwt 토큰
     * @param - commentId(Long) : 삭제 요청할 댓글/대댓글 id
     * @author - Tae hyun Park
     * @since - 2022-09-10
     */
    private fun processPostCommentDelete(
        token: String,
        commentId: Long,
        last: (Boolean) -> Unit,
        error: (ResponseErrorException) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startPostCommentDelete(
                    token,
                    commentId
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            } catch (e: ResponseErrorException) {
                error(e)
            }
        }
    }

    /**
     * 아이템 하나를 리스트에 추가하는 함수
     * @param item(ResponseComment?): 댓글 아이템 객체 하나 (nullable)
     * @author Tae hyun Park
     * @since 2022-09-01
     */
    fun addItem(item: ResponseComment?) {
        data.add(item)
        notifyItemInserted(itemCount)
    }

    /**
     * 리스트에 비우는 함수
     * @author Tae hyun Park
     * @since 2022-09-01
     */
    fun clearList() {
        data.clear()
        notifyDataSetChanged()
    }

    /**
     * 아이템 리스트를 리스트에 추가하는 함수
     * @param items(List<ResponseComment>): 댓글 객체 리스트
     * @author Tae hyun Park
     * @since 2022-09-01
     */
    fun addItems(items: List<ResponseComment>) {
        data.addAll(items)
        notifyItemRangeInserted(itemCount, items.size)
    }

    /**
     * 리스트의 마지막 아이템을 지우는 함수
     * @author Tae hyun Park
     * @since 2022-09-01
     */
    fun removeLast() {
        data.removeAt(data.size - 1)
        notifyItemRemoved(data.size)
    }

    /**
     * 현재 아이템을 새로운 아이템으로 대체하는 함수
     * @author Tae hyun Park
     * @since 2022-09-01
     */
    fun replaceAll(items: List<ResponseComment>) {
        data.clear()
        data.addAll(items)
        notifyDataSetChanged()
    }

    /**
     * 아이템 view type 을 가져옴
     */
    override fun getItemViewType(position: Int): Int {
        return if (data[position] == null) ValueUtil.VIEW_TYPE_LOADING else ValueUtil.VIEW_TYPE_ITEM
    }

    /**
     * @description - 해당 대댓글의 해시태그를 강조하여 보여주는 함수
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-25 | 2022-09-03
     */
    private fun setViewHashTag(commentContent: TextView, content: String) {
        // 해시 태그 리스트 색상 표시
        var hashTagList = Utils.getArrayHashTagWithOutSpace(content)
        var spannableString = SpannableString(content) // 텍스트 뷰의 특정 문자열 처리를 위한 spannableString 객체 생성
        var startList = ArrayList<Int>()
        for (hashString in hashTagList) {
            var start = content.indexOf("#$hashString") // 전체 문자열에서 해당 해시태그 문자열과 일치하는 첫 인덱스를 찾아낸다
            for (listIndex in startList) {
                if (start == listIndex)// 중복된 태그가 이미 있다면
                    start = content.indexOf("#$hashString", start + 1) // 중복이므로 그 다음 인덱스부터 다시 찾는다
            }
            startList.add(start) // 인덱스들을 저장
            var end = start + hashString.length // 해당 해시태그 문자열의 끝 인덱스
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#014D91")),
                start,
                end + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) // spannable 속성 지정
        }
        startList.clear()
        commentContent.text = spannableString
    }

}