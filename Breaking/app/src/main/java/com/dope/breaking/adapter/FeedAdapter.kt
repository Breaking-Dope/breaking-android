package com.dope.breaking.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.dope.breaking.R
import com.dope.breaking.model.response.ResponseMainFeed
import com.dope.breaking.util.DateUtil
import com.dope.breaking.util.ValueUtil
import java.text.DecimalFormat

class FeedAdapter(
    private val context: Context,
    var data: MutableList<ResponseMainFeed?>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val decimalFormat = DecimalFormat("#,###") // 숫자 콤마 포맷을 위한 클래스

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
        notifyItemInserted(itemCount)
    }

    /**
     * 리스트에 비우는 함수
     * @author Seunggun Sin
     * @since 2022-08-15
     */
    fun clearList() {
        data.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    /**
     * 아이템 리스트를 리스트에 추가하는 함수
     * @param items(List<ResponseMainFeed>): 피드 객체 리스트
     * @author Seunggun Sin
     * @since 2022-08-10
     */
    fun addItems(items: List<ResponseMainFeed>) {
        data.addAll(items)
        notifyItemRangeInserted(itemCount, items.size)
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
        return if (data[position] == null) ValueUtil.VIEW_TYPE_LOADING else ValueUtil.VIEW_TYPE_ITEM
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
            title.text = item.title
            likeCount.text = item.likeCount.toString()
            commentCount.text = item.commentCount.toString()
            price.text = decimalFormat.format(item.price) + "원"
            chipExclusive.visibility = if (item.postType == "EXCLUSIVE") View.VISIBLE else View.GONE

            if (item.postType == "FREE" || item.price == 0)
                price.text = "무료"

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
}