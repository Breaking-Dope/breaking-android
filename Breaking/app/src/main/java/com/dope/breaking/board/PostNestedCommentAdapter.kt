package com.dope.breaking.board

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dope.breaking.R
import com.dope.breaking.model.response.ResponseComment
import com.dope.breaking.util.DateUtil
import com.dope.breaking.util.Utils
import com.dope.breaking.util.ValueUtil

class PostNestedCommentAdapter (
    private val context: Context,
    var data: MutableList<ResponseComment?>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ValueUtil.VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_nested_comment_list, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder)
            holder.bind(data[position]!!)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view){
        private val commentProfile = itemView.findViewById<ImageView>(R.id.iv_comment_profile_nested)
        private val commentNickname = itemView.findViewById<TextView>(R.id.tv_comment_nickname_nested)
        private val commentContent = itemView.findViewById<TextView>(R.id.tv_comment_content_nested)
        private val commentButtonLike = itemView.findViewById<ImageButton>(R.id.ib_post_like_nested)
        private val commentCountLike = itemView.findViewById<TextView>(R.id.tv_post_like_count_nested)
        private val commentButtonMore = itemView.findViewById<ImageButton>(R.id.ib_post_more_nested)
        private val commentDate = itemView.findViewById<TextView>(R.id.tv_post_time_nested)

        fun bind(item: ResponseComment){
            var isContentButtonPressed = false // 댓글 본문이 눌렸는지 안 눌렸는지

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
            commentNickname.text = item.user.nickname
            commentContent.text = item.content
            commentCountLike.text = item.likeCount.toString()
            commentDate.text = DateUtil().getTimeDiff(item.createdDate)
            setViewHashTag(commentContent, commentContent.text.toString()) // 대댓글 해시태그 강조

            /* 좋아요 구현 예정 */

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
        }
    }

    inner class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val progressDialog = itemView.findViewById<ProgressBar>(R.id.progressbar_loading)
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