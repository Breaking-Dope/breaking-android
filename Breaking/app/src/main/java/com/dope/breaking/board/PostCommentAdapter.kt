package com.dope.breaking.board

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.dope.breaking.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PostCommentAdapter (
    private val context: Context,
    var data: MutableList<ResponseComment?>,
    var token: String,
    var getPostId: Long,
    var binding : ActivityPostDetailBinding
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var isReplyButtonPressed = false // 답글 달기 버튼이 눌렸는지 안 눌렸는지
    private lateinit var itemReplyClickListener: OnItemClickListener // 아이템 리스트 클릭 리스너
    private lateinit var adapterNestedComment: PostNestedCommentAdapter // 대댓글 리스트 어댑터
    private var isObtainedAll = false // 모든 대댓글 리스트를 받았는지 판단 (더 이상 요청할 것이 없는)
    private var isLoading = false  // 로딩 중 판단
    private var commentCountRefresh = 0
    private val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager // 키보드 제어 위한 inputManager 선언

    // 대댓글 요청 에러 시 띄워줄 다이얼로그 정의
    val requestErrorDialog =
        DialogUtil().SingleDialog(context, "답글을 가져오는데 문제가 발생하였습니다.", "확인")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ValueUtil.VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_comment_list, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(data[position]!!)
            holder.commentProfile.setOnClickListener { // 댓글 프로필 클릭 시 이동
                UserProfile(context as Activity).moveToUserPage(data[position]!!.user.userId)
            }
            holder.commentNickname.setOnClickListener { // 댓글 닉네임 클릭 시 이동
                UserProfile(context as Activity).moveToUserPage(data[position]!!.user.userId)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val commentProfile = itemView.findViewById<ImageView>(R.id.iv_comment_profile)
        val commentNickname = itemView.findViewById<TextView>(R.id.tv_comment_nickname)
        private val commentContent = itemView.findViewById<TextView>(R.id.tv_comment_content)
        private val commentButtonLike = itemView.findViewById<ImageButton>(R.id.ib_post_like)
        private val commentCountLike = itemView.findViewById<TextView>(R.id.tv_post_like_count)
        private val commentButtonReply = itemView.findViewById<TextView>(R.id.tv_post_reply)
        private val commentMoreReply = itemView.findViewById<TextView>(R.id.tv_post_reply_count)
        private val commentDate = itemView.findViewById<TextView>(R.id.tv_post_time)

        private val nestedRecyclerView = itemView.findViewById<RecyclerView>(R.id.rv_nested_comment_list)
        private val moreMenu = itemView.findViewById<ImageButton>(R.id.ib_post_more_comment)

        private val commentInput = itemView.findViewById<EditText>(R.id.et_comment_content)
        private val commentCancel = itemView.findViewById<TextView>(R.id.tv_cancel)
        private val commentRegister = itemView.findViewById<TextView>(R.id.tv_register)
        private val constraintSet = itemView.findViewById<ConstraintLayout>(R.id.constraint_view)

        fun bind(item: ResponseComment){
            var isReplyViewPressed = false // 답변 보기 버튼이 눌렸는지 안 눌렸는지
            var isContentButtonPressed = false // 댓글 본문이 눌렸는지 안 눌렸는지
            var nestedCommentList = mutableListOf<ResponseComment?>() // 대댓글 어댑터에 넣어줄 대댓글 리스트 데이터
            adapterNestedComment = PostNestedCommentAdapter(context, token, nestedCommentList, item.commentId.toLong(), binding)
            allowScrollRecyclerView(itemView) // 리사이클러뷰 스크롤 중첩 문제 해결

            if(item.user.profileImgUrl == null){
                Glide.with(itemView)
                    .load(R.drawable.ic_default_profile_image)
                    .circleCrop()
                    .fitCenter()
                    .into(commentProfile)
            }else{
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

            if (item.user.userId == ResponseExistLogin.baseUserInfo?.userId){ // 내 댓글이면 수정, 삭제 메뉴가 보이게
                popupBind.layoutHorizEditComment.visibility = View.VISIBLE
                popupBind.layoutHorizDeleteComment.visibility = View.VISIBLE
                popupBind.layoutHorizChatComment.visibility = View.GONE
                popupBind.layoutHorizBanComment.visibility = View.GONE
            }else{ // 타 유저 댓글이면 채팅, 차단 메뉴가 보이게
                popupBind.layoutHorizEditComment.visibility = View.GONE
                popupBind.layoutHorizDeleteComment.visibility = View.GONE
                popupBind.layoutHorizChatComment.visibility = View.VISIBLE
                popupBind.layoutHorizBanComment.visibility = View.VISIBLE
            }

            // 수정 뷰에서 취소 버튼 누르면 기존 뷰 원상복구
            commentCancel.setOnClickListener {
                changeViewCommentModifyBefore(item) // 기존 뷰로 폼 변경
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
                    "댓글을 삭제하시겠습니까?",
                    "예",
                    "아니오",
                    {
                        // 댓글 삭제 요청 함수 시작
                        processPostCommentDelete(
                            token,
                            item.commentId.toLong(),{
                                if(it){
                                    Toast.makeText(context,"삭제되었습니다.",Toast.LENGTH_SHORT).show()
                                    // 댓글 갱신
                                    processGetCommentList(
                                        token,
                                        getPostId,
                                        0,{
                                            replaceAll(it)
                                            commentCountRefresh += it.size
                                            for(i in it.indices){
                                                commentCountRefresh += it[i].replyCount
                                            }
                                            binding.tvPostCommentCount.text = commentCountRefresh.toString()
                                            commentCountRefresh = 0 // 댓글 개수 다시 초기화
                                        },{
                                            it.printStackTrace()
                                            DialogUtil().SingleDialog(context, "댓글 갱신에 문제가 발생하였습니다.", "확인")
                                        }
                                    )
                                }
                            },{
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
                if(commentInput.text.toString().isNotEmpty()){
                    processPostCommentEdit(
                        token,
                        item.commentId.toLong(),
                        RequestComment(
                            commentInput.text.toString(),
                            Utils.getArrayHashTagWithOutSpace(commentInput.text.toString())
                        ),{ it ->
                            if (it){
                                Toast.makeText(context,"수정이 완료되었습니다.",Toast.LENGTH_SHORT).show()
                                changeViewCommentModifyBefore(item) // 원래 댓글 화면으로 뷰 변화 주기
                                // 댓글 갱신
                                processGetCommentList(
                                    token,
                                    getPostId,
                                    0,{
                                        replaceAll(it)
                                        commentCountRefresh += it.size
                                        for(i in it.indices){
                                            commentCountRefresh += it[i].replyCount
                                        }
                                        binding.tvPostCommentCount.text = commentCountRefresh.toString()
                                        commentCountRefresh = 0 // 댓글 개수 다시 초기화
                                    },{
                                        it.printStackTrace()
                                        DialogUtil().SingleDialog(context, "댓글 갱신에 문제가 발생하였습니다.", "확인")
                                    }
                                )
                                // 대댓글 갱신
                                processGetNestedCommentList(
                                    token,
                                    item.commentId.toLong(),
                                    0,{
                                        isObtainedAll = false
                                    },{
                                        // 대댓글의 경우 현재 수정한 대댓글만 갱신 되도록 함.
                                        // replaceAll(it)시에 현재 열려 있는 각 답글들의 뷰가 겹치는 문제 발생
                                        adapterNestedComment = PostNestedCommentAdapter(context, token, it as MutableList<ResponseComment?>, item.commentId.toLong(), binding)
                                        nestedRecyclerView.adapter = adapterNestedComment
                                    },{
                                        it.printStackTrace()
                                        DialogUtil().SingleDialog(context, "답글 갱신에 문제가 발생하였습니다.", "확인")
                                    }
                                )
                            }
                        },{
                            it.printStackTrace()
                            DialogUtil().SingleDialog(context, "댓글 수정 요청에 문제가 발생하였습니다.", "확인")
                        }
                    )
                }else{
                    Toast.makeText(context,"수정할 내용을 입력해주세요!",Toast.LENGTH_SHORT).show()
                }
            }

            commentNickname.text = item.user.nickname
            commentContent.text = item.content
            if(item.isLiked) // 내가 좋아요가 누른 상태면
                commentButtonLike.background = ContextCompat.getDrawable(context,R.drawable.ic_post_like_after)
            else // 아니라면
                commentButtonLike.background = ContextCompat.getDrawable(context,R.drawable.ic_post_like)
            commentCountLike.text = item.likeCount.toString()
            commentDate.text = DateUtil().getTimeDiff(item.createdDate)
            setViewHashTag(commentContent, commentContent.text.toString()) // 댓글에 해시태그 강조

            if (item.replyCount > 0) { // 답글의 개수가 1개 이상 있다면
                commentMoreReply.visibility = View.VISIBLE // 답글 더보기 보이기
                commentMoreReply.text = "답글 ${item.replyCount}개" // 답글 더보기 부분
            }else{
                commentMoreReply.visibility = View.GONE // 답글 더보기 숨기기
            }

            if(nestedRecyclerView.visibility == 0){
                isReplyViewPressed = true
                commentMoreReply.text = "답글 닫기" // 답글 더보기 부분
            }else{
                isReplyViewPressed = false
                if(item.replyCount > 0){
                    commentMoreReply.text = "답글 ${item.replyCount}개" // 답글 더보기 부분
                }
            }

            // 대댓글 즉, 답글 더보기 클릭 리스너
            commentMoreReply.setOnClickListener {
                if(isReplyViewPressed){ // 다시 눌러서 답글 리스트가 닫혀야 하는 경우
                    isReplyViewPressed = false // 답변 보기가 닫힌 상태로 전환
                    nestedRecyclerView.visibility = View.GONE // 대댓글 리사이클러 뷰 gone 처리
                    nestedCommentList.clear()
                    //adapterNestedComment.clearList() // 대댓글 리스트 데이터 전체 비우기

                    // 대댓글 리스트 요청
                    processGetNestedCommentList(
                        token,
                        item.commentId.toLong(),
                        0,{},{
                            if(it.isEmpty())
                                commentMoreReply.visibility = View.GONE
                            else {
                                commentMoreReply.visibility = View.VISIBLE
                                commentMoreReply.text = "답글 ${it.size}개"
                            }
                        },{
                            it.printStackTrace()
                            requestErrorDialog.show()
                        })
                } else if(commentMoreReply.visibility == 0 && !isReplyViewPressed){ // 대댓글 요청을 할 필요가 있다면 (답글 버튼이 활성화 되어 있고, 답글 보기가 닫힌 상태라면)
                    nestedCommentList.clear()
                    //adapterNestedComment.clearList()
                    commentMoreReply.text = "답글 닫기" // 답글 더보기 부분
                    // 대댓글 리스트 요청
                    processGetNestedCommentList(
                        token,
                        item.commentId.toLong(),
                        0,{
                            isObtainedAll = false
                        },{
                            isReplyViewPressed = true // 답변 보기가 열린 상태로 전환
                            nestedRecyclerView.visibility = View.VISIBLE // 대댓글 리사이클러 뷰 visible 처리

                            nestedCommentList.addAll(it)
                            //adapterNestedComment.addItems(it)

                            adapterNestedComment = PostNestedCommentAdapter(context, token, nestedCommentList, item.commentId.toLong(), binding)
                            nestedRecyclerView.adapter = adapterNestedComment // 어댑터 지정
                        },{
                            it.printStackTrace()
                            requestErrorDialog.show()
                        })
                }
            }

            /* 대댓글 RecyclerView 스크롤 이벤트 리스너 정의 */
            nestedRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    // 스크롤하면서 리스트의 가장 마지막 위치에 도달했을 때, 그 인덱스 값 가져오기
                    val lastIndex =
                        (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                    // 가져온 아이템 사이즈가 가져와야하는 사이즈보다 작은 경우 새로운 요청을 못하게 막기
                    if (recyclerView.adapter!!.itemCount < ValueUtil.NESTED_COMMENT_SIZE) {
                        return
                    }

                    // 실제 데이터 리스트의 마지막 인덱스와 스크롤 이벤트에 의한 인덱스 값이 같으면서
                    // 스크롤이 드래깅 중이면서
                    // 대댓글 리스트 요청이 더 가능하면서
                    // 로딩 중이 아니라면
                    if (lastIndex == recyclerView.adapter!!.itemCount - 1 && newState == 2 && !isObtainedAll && !isLoading) {
                        processGetNestedCommentList(token, item.commentId.toLong(), nestedCommentList[lastIndex]!!.commentId, {
                            adapterNestedComment.addItem(null) // 로딩 창 아이템 추가
                            isLoading= true // 로딩 시작 상태로 전환
                        },{
                            if(it.size < ValueUtil.NESTED_COMMENT_SIZE){ // 정량으로 가져오는 개수보다 적다면
                                adapterNestedComment.removeLast() // 로딩 아이템 제거
                                if (it.isNotEmpty()) // 리스트가 있다면
                                    adapterNestedComment.addItems(it)
                                isObtainedAll = true // 더 이상 받아올 대댓글이 없는 상태
                            }else{ // 더 요청할 수 있고 받아온 리스트가 있다면
                                adapterNestedComment.removeLast() // 로딩 아이템 제거
                                adapterNestedComment.addItems(it) // 받아온 리스트 추가
                            }
                            isLoading = false // 로딩 종료 상태로 전환
                        },{
                            // BSE451 에러의 경우 더 이상의 받아올 댓글(대댓글) 리스트가 없는 경우 발생
                            if (it.message!!.contains("BSE451")) {
                                isObtainedAll = true
                                adapterNestedComment.removeLast()
                            }
                        })
                    }
                }
            })

            /* 댓글 좋아요 구현  */
            commentButtonLike.setOnClickListener {
                if (!item.isLiked){ // 좋아요가 안 눌렸다면 좋아요 요청
                    processPostCommentLike(
                        token,
                        item.commentId.toLong(),{
                            if(it){
                                Log.d("PostCommentAdapter.kt","좋아요 성공")
                                commentButtonLike.background = ContextCompat.getDrawable(context,R.drawable.ic_post_like_after)
                                commentCountLike.text = (item.likeCount + 1).toString()
                                item.isLiked = true
                                item.likeCount = item.likeCount + 1
                            }
                        },{
                            it.printStackTrace()
                            DialogUtil().SingleDialog(context, "좋아요 요청에 문제가 발생하였습니다.", "확인")
                        }
                    )
                }else{ // 좋아요가 눌렸다면 좋아요 취소 요청
                    processPostCommentUnLike(
                        token,
                        item.commentId.toLong(),{
                            if(it){
                                Log.d("PostCommentAdapter.kt","좋아요 취소 성공")
                                commentButtonLike.background = ContextCompat.getDrawable(context,R.drawable.ic_post_like)
                                commentCountLike.text = (item.likeCount - 1).toString()
                                item.isLiked = false
                                item.likeCount = item.likeCount - 1
                            }
                        },{
                            it.printStackTrace()
                            DialogUtil().SingleDialog(context, "좋아요 취소 요청에 문제가 발생하였습니다.", "확인")
                        }
                    )
                }
            }

            /* 더보기 메뉴 구현 */
            commentContent.setOnClickListener { // 클릭 시
                if (!isContentButtonPressed){
                    commentContent.maxLines = 10 // 다 보이게 (10줄로 기본 설정)
                    isContentButtonPressed = true
                } else{
                    commentContent.maxLines = 2 // 다시 2줄로 설정
                    isContentButtonPressed = false
                }
            }

            // 답글 달기 버튼 클릭 리스너
            commentButtonReply.setOnClickListener { // 답글 달기 버튼을 눌렀다면
                if(!isReplyButtonPressed) { // 처음 답글 달기 버튼을 눌렀다면
                    isReplyButtonPressed = true // 답글 달기 활성화
                    commentButtonReply.text = "답글 작성 중.."
                    binding.etPostWrite.setText("@${item.user.nickname} ") // 댓글 입력 창에 유저 태그 붙여주기(@)
                    // 유저 태그 강조 표시하기
                    var spannableString = SpannableString(binding.etPostWrite.text) // 텍스트 뷰의 특정 문자열 처리를 위한 spannableString 객체 생성
                    var start = binding.etPostWrite.text.indexOf("@${item.user.nickname}") // 전체 문자열에서 해당 해시태그 문자열과 일치하는 첫 인덱스를 찾아낸다
                    var end = start + item.user.nickname.length // 해당 해시태그 문자열의 끝 인덱스
                    spannableString.setSpan(
                        ForegroundColorSpan(Color.parseColor("#014D91")),
                        start,
                        end + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // spannable 속성 지정
                    binding.etPostWrite.setText(spannableString)
                    binding.etPostWrite.setSelection(binding.etPostWrite.length()) // 커서 맨 마지막에 위치
                    binding.etPostWrite.requestFocus() // 입력 창 Focus 효과
                    inputMethodManager.showSoftInput(binding.etPostWrite, 0) // 키보드 올리기
                    itemReplyClickListener.onClick(it, adapterPosition) // 답글 달기가 true 일 때, commentId를 다루기 위해 리스너 등록
                } else{ // 다시 누른다면
                    if(commentButtonReply.text.equals("답글 작성 중..")){
                        isReplyButtonPressed = false // 답글 달기 비활성화
                        commentButtonReply.text = "답글"
                        binding.etPostWrite.setText("") // 댓글 입력 창 원래대로 표시
                        binding.etPostWrite.clearFocus() // 입력 창 Focus 제거
                        inputMethodManager.hideSoftInputFromWindow(binding.etPostWrite.windowToken, 0) // 키보드 내리기
                        itemReplyClickListener.onClick(it, adapterPosition) // 답글 달기가 true 일 때, commentId를 다루기 위해 리스너 등록
                    }else{
                        Toast.makeText(context, "한 번에 하나의 대댓글만 작성할 수 있어요!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        /**
         * @description - 댓글/대댓글 수정 시에 뷰를 바꾸는 메소드 (Before)
         * @return - None
         * @author - Tae hyun Park
         * @since - 2022-09-10
         */
        private fun changeViewCommentModifyBefore(item: ResponseComment){
            // 기존 뷰 VISIBLE
            commentNickname.visibility = View.VISIBLE
            commentContent.visibility = View.VISIBLE
            commentButtonLike.visibility = View.VISIBLE
            commentCountLike.visibility = View.VISIBLE
            commentButtonReply.visibility = View.VISIBLE
            if(item.replyCount > 0)
                commentMoreReply.visibility = View.VISIBLE
            commentDate.visibility = View.VISIBLE
            moreMenu.visibility = View.VISIBLE
            // 수정 뷰 GONE
            commentInput.visibility = View.GONE
            commentCancel.visibility = View.GONE
            commentRegister.visibility = View.GONE
            // 종속성 변경
            val constraintLayout = constraintSet
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.connect(nestedRecyclerView.id, ConstraintSet.TOP, commentMoreReply.id, ConstraintSet.BOTTOM)
            constraintSet.applyTo(constraintLayout)
        }

        /**
         * @description - 댓글/대댓글 수정 시에 뷰를 바꾸는 메소드 (After)
         * @return - None
         * @author - Tae hyun Park
         * @since - 2022-09-10
         */
        private fun changeViewCommentModifyAfter(popupWindow: PopupWindow){
            // 수정 뷰를 띄우기 위해 기존 뷰 GONE
            commentNickname.visibility = View.GONE
            commentContent.visibility = View.GONE
            commentButtonLike.visibility = View.GONE
            commentCountLike.visibility = View.GONE
            commentButtonReply.visibility = View.GONE
            commentMoreReply.visibility = View.GONE
            commentDate.visibility = View.GONE
            moreMenu.visibility = View.GONE
            // 수정 뷰 VISIBLE
            commentInput.visibility = View.VISIBLE
            commentCancel.visibility = View.VISIBLE
            commentRegister.visibility = View.VISIBLE
            // 종속성 변경
            val constraintLayout = constraintSet
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.connect(nestedRecyclerView.id, ConstraintSet.TOP, commentCancel.id, ConstraintSet.BOTTOM)
            constraintSet.applyTo(constraintLayout)
            popupWindow.dismiss()
            // 수정 전 기존 내용 그대로 보여주기
            commentInput.setText(commentContent.text.toString())
        }

    }

    inner class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val progressDialog = itemView.findViewById<ProgressBar>(R.id.progressbar_loading)
    }

    /**
     * @description - 게시물의 댓글 리스트 요청 함수를 호출하는 메소드
     * @param - token(String) : JWT 토큰
     * @param - postId(Long) : 댓글 리스트를 요청할 게시물 id
     * @param - lastCommentId(Int) : 가장 최근에 요청한 댓글 id
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-01
     */
    private fun processGetCommentList(
        token: String,
        postId : Long,
        lastCommentId : Int,
        last: (List<ResponseComment>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ){
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startGetCommentList(
                    token,
                    postId,
                    lastCommentId,
                    ValueUtil.COMMENT_SIZE,
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
                Log.d("PostCommentAdapter.kt", "댓글 리스트 요청 결과 : $response")
            }catch (e: ResponseErrorException){
                error(e)
            }
        }
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
        commentId : Long,
        lastCommentId : Int,
        init: () -> Unit,
        last: (List<ResponseComment>) -> Unit,
        error: (ResponseErrorException) -> Unit
    ){
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
            }catch (e: ResponseErrorException){
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
    ){
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startPostCommentLike(
                    token,
                    commentId
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            }catch (e: ResponseErrorException){
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
    ){
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startPostCommentUnLike(
                    token,
                    commentId
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            }catch (e: ResponseErrorException){
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
    ){
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startPostCommentEdit(
                    token,
                    commentId,
                    commentInfo
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            }catch (e: ResponseErrorException){
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
    ){
        CoroutineScope(Dispatchers.Main).launch {
            val postManager = PostManager() // 커스텀 게시글 객체 생성
            try {
                val response = postManager.startPostCommentDelete(
                    token,
                    commentId
                )
                last(response) // 받아온 리스트를 바탕으로 후처리 함수 호출
            }catch (e: ResponseErrorException){
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

    // 액티비티에서 클릭 이벤트 오버라이드 하기 위해 인터페이스 정의
    interface OnItemClickListener {
        fun onClick(v: View, position: Int)
    }

    // 아이템 중 답글 달기 클릭 리스너 함수
    fun setItemReplyClickListener(onItemClickListener: OnItemClickListener) {
        this.itemReplyClickListener = onItemClickListener // 액티비티에서 구현한 인터페이스 정보를 할당
    }

    /**
     * @description - 댓글과 대댓글 두 개의 리사이클러뷰의 스크롤이 중첩되는 문제를 해결하기 위해, 대댓글 스크롤인 경우, 댓글 스크롤 권한을 가로채고
     * 댓글 스크롤인 경우 대댓글 스크롤 권한을 다시 가로채는 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-02
     */
    private fun allowScrollRecyclerView(view: View) {
        // RecyclerView 스크롤 터지 이벤트 리스너
        view.findViewById<RecyclerView>(R.id.rv_nested_comment_list).setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (v!!.id === R.id.rv_nested_comment_list) { // 안쪽 리사이클러뷰를 클릭했다면
                    v!!.parent.requestDisallowInterceptTouchEvent(true) // 부모 뷰의 스크롤 이벤트 비허용
                    when (event!!.action and MotionEvent.ACTION_MASK) { // 만약 바깥 이벤트가 클릭되었다면
                        MotionEvent.ACTION_UP -> v!!.parent.requestDisallowInterceptTouchEvent(false) // 부모 뷰의 스크롤 이벤트 허용
                    }
                }
                return false
            }
        })
    }

    /**
     * @description - 해당 댓글의 해시태그를 강조하여 보여주는 함수
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-25 | 2022-09-03
     */
    private fun setViewHashTag(commentContent: TextView, content: String){
        // 해시 태그 리스트 색상 표시
        var hashTagList = Utils.getArrayHashTagWithOutSpace(content)
        var spannableString = SpannableString(content) // 텍스트 뷰의 특정 문자열 처리를 위한 spannableString 객체 생성
        var startList = ArrayList<Int>()
        for(hashString in hashTagList){
            var start = content.indexOf("#$hashString") // 전체 문자열에서 해당 해시태그 문자열과 일치하는 첫 인덱스를 찾아낸다
            for(listIndex in startList){
                if(start == listIndex)// 중복된 태그가 이미 있다면
                    start = content.indexOf("#$hashString",start+1) // 중복이므로 그 다음 인덱스부터 다시 찾는다
            }
            startList.add(start) // 인덱스들을 저장
            var end = start + hashString.length // 해당 해시태그 문자열의 끝 인덱스
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#014D91")),
                start,
                end + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // spannable 속성 지정
        }
        startList.clear()
        commentContent.text = spannableString
    }
}