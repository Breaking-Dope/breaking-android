package com.dope.breaking.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dope.breaking.R
import com.dope.breaking.UserPageActivity
import com.dope.breaking.model.FollowData
import com.dope.breaking.util.ValueUtil

class FollowAdapter(private val context: Context, var data: MutableList<FollowData>) :
    RecyclerView.Adapter<FollowAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_follow_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowAdapter.ViewHolder, position: Int) {
        holder.bind(data[position])
        // 닉네임 클릭 시
        holder.textNickname.setOnClickListener {
            context.startActivity(Intent(context, UserPageActivity::class.java))
        }
        // 프로필 이미지 클릭 시
        holder.imageProfile.setOnClickListener {
            context.startActivity(Intent(context, UserPageActivity::class.java))

        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNickname: TextView = itemView.findViewById(R.id.tv_following_nickname)
        val textStatus: TextView = itemView.findViewById(R.id.tv_following_status)
        val imageProfile: ImageView = itemView.findViewById(R.id.imgv_following_profile)
        val removeButton: Button = itemView.findViewById(R.id.btn_following_remove)

        fun bind(item: FollowData) {
            textNickname.text = item.nickname
            textStatus.text = item.statusMsg
            Glide.with(itemView).load(ValueUtil.IMAGE_BASE_URL + item.profileImgURL)
                .circleCrop().error(R.drawable.ic_default_profile_image)
                .into(imageProfile)
        }
    }
}