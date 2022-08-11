package com.dope.breaking.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

class UserPostAdapter(private val context: Context, var data: MutableList<ResponseMainFeed>) :
    RecyclerView.Adapter<UserPostAdapter.ViewHolder>() {
    private val decimalFormat = DecimalFormat("#,###") // 숫자 콤마 포맷을 위한 클래스
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserPostAdapter.ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
}