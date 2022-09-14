package com.dope.breaking.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.dope.breaking.R
import com.dope.breaking.SignInActivity
import com.dope.breaking.databinding.CustomMainFeedPopupBinding
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.ResponseMainFeed
import com.dope.breaking.post.PostManager
import com.dope.breaking.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class FeedAdapter(
    private val context: Context,
    var data: MutableList<ResponseMainFeed?>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val decimalFormat = DecimalFormat("#,###") // 숫자 콤마 포맷을 위한 클래스
    private lateinit var itemListClickListener: OnItemClickListener // 아이템 리스트 클릭 리스너

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ValueUtil.VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_post_list, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder)
            holder.bind(data[position]!!)
        // 아이템 리스트가 클릭된다면
        holder.itemView.setOnClickListener {
            itemListClickListener.onClick(it, position)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    /**
     * 아이템 하나를 리스트에 추가하는 함수
     * @param item(ResponseMainFeed?): 피드 아이템 객체 하나 (nullable)
     * @author Seunggun Sin
     * @since 2022-08-10 | 2022-08-18
     */
    fun addItem(item: ResponseMainFeed?) {
        data.add(item)
        notifyItemInserted(itemCount)
    }

    /**
     * 리스트에 비우는 함수
     * @author Seunggun Sin
     * @since 2022-08-15 | 2022-08-25
     */
    fun clearList() {
        data.clear()
        notifyDataSetChanged()
    }

    /**
     * 아이템 리스트를 리스트에 추가하는 함수
     * @param items(List<ResponseMainFeed>): 피드 객체 리스트
     * @author Seunggun Sin
     * @since 2022-08-10 | 20222-08-18
     */
    fun addItems(items: List<ResponseMainFeed>) {
        data.addAll(items)
        notifyItemRangeInserted(itemCount, items.size)
    }

    /**
     * 리스트의 마지막 아이템을 지우는 함수
     * @author Seunggun Sin
     * @since 2022-08-15 | 2022-08-18
     */
    fun removeLast() {
        data.removeAt(data.size - 1)
        notifyItemRemoved(data.size)
    }

    /**
     * 현재 아이템을 새로운 아이템으로 대체하는 함수
     * @author Seunggun Sin
     * @since 2022-08-18
     */
    fun replaceAll(items: List<ResponseMainFeed>) {
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

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val thumbnail = itemView.findViewById<ImageView>(R.id.imgv_post_thumbnail)
        private val title = itemView.findViewById<TextView>(R.id.tv_post_title)
        private val likeCount = itemView.findViewById<TextView>(R.id.tv_post_like_count)
        private val price = itemView.findViewById<TextView>(R.id.tv_post_price)
        private val chipExclusive = itemView.findViewById<TextView>(R.id.tv_chip_exclusive_main)
        private val chipSold = itemView.findViewById<TextView>(R.id.tv_chip_sold_main)
        private val chipUnsold = itemView.findViewById<TextView>(R.id.tv_chip_unsold_main)
        private val chipSoldStop = itemView.findViewById<TextView>(R.id.tv_chip_sold_stop_main)
        private val chipHidden = itemView.findViewById<TextView>(R.id.tv_chip_hidden_main)
        private val nickname = itemView.findViewById<TextView>(R.id.tv_post_nickname)
        private val date = itemView.findViewById<TextView>(R.id.tv_post_time)
        private val location = itemView.findViewById<TextView>(R.id.tv_post_location)
        private val commentCount = itemView.findViewById<TextView>(R.id.tv_post_comment_count)
        private val moreMenu = itemView.findViewById<ImageButton>(R.id.img_btn_post_more_menu)

        fun bind(item: ResponseMainFeed) {
            if (item.thumbnailImgURL == null) {
                Glide.with(itemView)
                    .load(R.drawable.ic_default_post_image_size_up)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                    .fitCenter()
                    .into(thumbnail)
            } else {
                Glide.with(itemView)
                    .load(ValueUtil.IMAGE_BASE_URL + item.thumbnailImgURL)
                    .placeholder(R.drawable.ic_default_post_image_size_up)
                    .error(R.drawable.ic_default_post_image_size_up)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                    .into(thumbnail)
            }

            val popupInflater =
                context.applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupBind =
                CustomMainFeedPopupBinding.inflate(popupInflater) // 커스텀 팝업 레이아웃 binding inflate

            if (item.isBookmarked) {
                popupBind.tvPopupBookmark.typeface = Typeface.DEFAULT_BOLD
                popupBind.tvPopupBookmark.setTextColor(context.getColor(R.color.breaking_color))
                popupBind.imgvPopupBookmark.setBackgroundResource(R.drawable.ic_baseline_bookmark_theme_24)
            } else {
                popupBind.tvPopupBookmark.typeface = Typeface.DEFAULT
                popupBind.tvPopupBookmark.setTextColor(context.getColor(R.color.black))
                popupBind.imgvPopupBookmark.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_theme_24)
            }

            val popupWindow = PopupWindow(
                popupBind.root,
                ViewGroup.LayoutParams.WRAP_CONTENT, // 가로 길이
                ViewGroup.LayoutParams.WRAP_CONTENT, // 세로 길이
                true
            ) // 팝업 윈도우 화면 설정

            moreMenu.setOnClickListener(popupWindow::showAsDropDown) // 더보기 메뉴 클릭 시, 메뉴 view 중심으로 팝업 메뉴 호출

            val errorDialog = DialogUtil().SingleDialog(context, "요청에 문제가 발생하였습니다.", "확인")

            // 수정 메뉴 클릭 시
            popupBind.layoutHorizEdit.setOnClickListener {
                popupWindow.dismiss()
            }

            // 삭제 메뉴 클릭 시
            popupBind.layoutHorizDelete.setOnClickListener {
                popupWindow.dismiss()
            }

            // 북마크 메뉴 클릭 시
            popupBind.layoutHorizBookmark.setOnClickListener {
                if (ResponseExistLogin.baseUserInfo == null) {
                    DialogUtil().MultipleDialog(
                        context,
                        "로그인이 필요합니다. 로그인 하러 가시겠습니까?",
                        "취소",
                        "이동",
                        {},
                        {
                            context.startActivity(Intent(context, SignInActivity::class.java))
                        }).show()
                    return@setOnClickListener
                }
                val postManager = PostManager()
                val token =
                    ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(context).getAccessTokenFromLocal()
                if (item.isBookmarked) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = postManager.startUnRegisterBookmark(item.postId, token)
                        if (result) {
                            item.isBookmarked = false
                            popupBind.tvPopupBookmark.setTextColor(context.getColor(R.color.black))
                            popupBind.tvPopupBookmark.typeface = Typeface.DEFAULT
                            popupBind.imgvPopupBookmark.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_theme_24)
                        } else {
                            errorDialog.show()
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = postManager.startRegisterBookmark(item.postId, token)
                        if (result) {
                            item.isBookmarked = true
                            popupBind.tvPopupBookmark.setTextColor(context.getColor(R.color.breaking_color))
                            popupBind.tvPopupBookmark.typeface = Typeface.DEFAULT_BOLD
                            popupBind.imgvPopupBookmark.setBackgroundResource(R.drawable.ic_baseline_bookmark_theme_24)
                        } else {
                            errorDialog.show()
                        }
                    }
                }
            }

            // 공유 메뉴 클릭 시
            popupBind.layoutHorizShare.setOnClickListener {
                popupWindow.dismiss()
            }

            title.text = item.title.ifEmpty { "(제목 없음)" }
            likeCount.text = NumberUtil().countNumberFormatter(item.likeCount)
            commentCount.text = NumberUtil().countNumberFormatter(item.commentCount)
            price.text = "${decimalFormat.format(item.price)}원"

            // chip 초기화
            chipExclusive.visibility = View.GONE
            chipSold.visibility = View.GONE
            chipUnsold.visibility = View.GONE
            chipSoldStop.visibility = View.GONE

            // 게시글 타입 (단독, 판매완료, 판매중)
            if (item.postType != "EXCLUSIVE") { // 단독 제보가 아니라면
                chipExclusive.visibility = View.GONE // 단독 제보 비활성화
                chipSold.visibility = View.GONE // 판매 완료 다시 비활성화
            } else
                chipExclusive.visibility = View.VISIBLE // 단독 제보 다시 활성화

            if (item.isSold && item.postType == "EXCLUSIVE") { // 단독 제보이고, 적어도 하나가 팔렸다면 판매 완료로 간주
                chipExclusive.visibility = View.VISIBLE // 단독 제보 활성화
                chipSold.visibility = View.VISIBLE // 판매 완료 다시 활성화
                chipUnsold.visibility = View.GONE // 판매 중 비활성화
            } else { // 판매완료가 아니라면 판매중, 판매중지로 간주
                chipSold.visibility = View.GONE // 판매 완료 다시 비활성화
                if (!item.isPurchasable) { // 판매 중지라면
                    chipSoldStop.visibility = View.VISIBLE // 판매 중지 활성화
                    chipUnsold.visibility = View.GONE // 판매 중 비활성화
                } else { // 판매 중
                    chipSoldStop.visibility = View.GONE // 판매 중지 비활성화
                    chipUnsold.visibility = View.VISIBLE // 판매 중 다시 활성화
                }
            }

            if (item.isHidden) // 숨겨진 게시물이면 태그 보여주기
                chipHidden.visibility = View.VISIBLE
            else
                chipHidden.visibility = View.GONE

            if (item.postType == "FREE" || item.price == 0) price.text = "무료"

            location.text =
                item.location.region_1depth_name + " " + item.location.region_2depth_name
            nickname.text = if (item.user == null) "익명" else item.user.nickname
            date.text = DateUtil().getTimeDiff(item.createdDate)
        }
    }

    inner class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val progressDialog = itemView.findViewById<ProgressBar>(R.id.progressbar_loading)
    }

    // 액티비티에서 클릭 이벤트 오버라이드 하기 위해 인터페이스 정의
    interface OnItemClickListener {
        fun onClick(v: View, position: Int)
    }

    // 아이템 리스트 클릭 리스너 함수
    fun setItemListClickListener(onItemClickListener: OnItemClickListener) {
        this.itemListClickListener = onItemClickListener // 액티비티에서 구현한 인터페이스 정보를 할당
    }
}