package com.dope.breaking.adapter

import android.content.Context
import android.graphics.Typeface
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
import com.dope.breaking.databinding.CustomMainFeedPopupBinding
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
    private val VIEW_TYPE_ITEM = 0 // 일반 아이템에 대한 레이아웃 view type
    private val VIEW_TYPE_LOADING = 1 // 로딩 아이템에 대한 레이아웃 view type
    private val decimalFormat = DecimalFormat("#,###") // 숫자 콤마 포맷을 위한 클래스
    private lateinit var itemListClickListener : FeedAdapter.OnItemClickListener // 아이템 리스트 클릭 리스너

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
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
     * @since 2022-08-10
     */
    fun addItem(item: ResponseMainFeed?) {
        data.add(item)
        notifyDataSetChanged()
    }

    /**
     * 리스트에 비우는 함수
     * @author Seunggun Sin
     * @since 2022-08-15
     */
    fun clearList() {
        data.clear()
        notifyDataSetChanged()
    }

    /**
     * 아이템 리스트를 리스트에 추가하는 함수
     * @param items(List<ResponseMainFeed>): 피드 객체 리스트
     * @author Seunggun Sin
     * @since 2022-08-10
     */
    fun addItems(items: List<ResponseMainFeed>) {
        data.addAll(items)
        notifyDataSetChanged()
    }

    /**
     * 리스트의 마지막 아이템을 지우는 함수
     * @author Seunggun Sin
     * @since 2022-08-15
     */
    fun removeLast() {
        data.removeAt(data.size - 1)
        notifyItemRemoved(data.size)
    }

    /**
     * 아이템 view type 을 가져옴
     */
    override fun getItemViewType(position: Int): Int {
        return if (data[position] == null) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val thumbnail = itemView.findViewById<ImageView>(R.id.imgv_post_thumbnail)
        private val title = itemView.findViewById<TextView>(R.id.tv_post_title)
        private val likeCount = itemView.findViewById<TextView>(R.id.tv_post_like_count)
        private val price = itemView.findViewById<TextView>(R.id.tv_post_price)
        private val chipExclusive = itemView.findViewById<TextView>(R.id.tv_chip_exclusive)
        private val chipSold = itemView.findViewById<TextView>(R.id.tv_chip_sold)
        private val chipUnsold = itemView.findViewById<TextView>(R.id.tv_chip_unsold)
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

            title.text = item.title
            likeCount.text = NumberUtil().countNumberFormatter(item.likeCount)
            commentCount.text = NumberUtil().countNumberFormatter(item.commentCount)
            price.text = "${decimalFormat.format(item.price)}원"
            chipExclusive.visibility = if (item.postType == "EXCLUSIVE") View.VISIBLE else View.GONE

            if (item.postType == "FREE" || item.price == 0) price.text = "무료"

            location.text = item.location.region_2depth_name
            chipSold.visibility = if (item.isSold) View.VISIBLE else View.GONE
            chipUnsold.visibility = if (item.isSold) View.GONE else View.VISIBLE
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
    fun setItemListClickListener(onItemClickListener: FeedAdapter.OnItemClickListener) {
        this.itemListClickListener = onItemClickListener // 액티비티에서 구현한 인터페이스 정보를 할당
    }
}