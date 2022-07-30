package com.dope.breaking.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dope.breaking.R
import com.dope.breaking.model.FollowData
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.user.UserProfile
import com.dope.breaking.util.ValueUtil

class FollowAdapter(private val context: Context, var data: MutableList<FollowData>) :
    RecyclerView.Adapter<FollowAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_follow_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowAdapter.ViewHolder, position: Int) {
        holder.bind(data[position]) // view 바인딩

        // 닉네임 클릭 시
        holder.textNickname.setOnClickListener {
            UserProfile(context as Activity).moveToUserPage(data[position].userId)
        }
        // 프로필 이미지 클릭 시
        holder.imageProfile.setOnClickListener {
            UserProfile(context as Activity).moveToUserPage(data[position].userId)
        }
        holder.removeButton.visibility =
            if (data[position].userId == ResponseExistLogin.baseUserInfo?.userId) View.GONE else View.VISIBLE
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