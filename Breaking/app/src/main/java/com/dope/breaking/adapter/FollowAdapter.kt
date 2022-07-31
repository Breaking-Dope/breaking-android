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

/**
 * 팔로우(워) 리스트 어댑터
 * @param context(Context): 리스트 호출하는 컨텍스트
 * @param data(MutableList<FollowData>): 응답으로 받아온 팔로우(워) 데이터 리스트
 * @param state(Boolean): 팔로우 리스트인지 팔로워 리스트인지 구분
 * @param currentUserId(Long): 팔로우(워) 리스트의 대상
 */
class FollowAdapter(
    private val context: Context,
    var data: MutableList<FollowData>,
    private val state: Boolean,
    private val currentUserId: Long
) :
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
        //TODO("팔로우, 팔로워 리스트에서 팔로우 & 언팔로우 구현해야함")
        if (currentUserId == ResponseExistLogin.baseUserInfo?.userId) { // 내 팔로우(워) 리스트인 경우
            if (state) { // 팔로우 리스트
                holder.removeButton.text = "팔로잉"
                holder.removeButton.setOnClickListener {
                    /*
                    언팔로우 dialog 띄우고 리스트에서 항목 삭제
                     */
//                    removeItem(position)
                }
            } else { // 팔로워 리스트
                /*
                상대방의 팔로우를 강제로 끊기
                 */
                holder.removeButton.text = "삭제"
            }
            holder.removeButton.setBackgroundResource(if (state) R.drawable.feed_category_button_background else R.drawable.sign_up_user_type_selected)
        } else { // 다른 사람 리스트인 경우
            /*
            각각 사람이 나와 팔로우 관계인지 아닌지 판단해서 "팔로우", "팔로잉" 구분
             */
            holder.removeButton.text = "팔로잉"
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

    /**
     * 리스트에서 아이템을 동적으로 지우는 함수
     * @param position(Int): 지우고자 하는 인덱스
     * @author Seunggun Sin
     * @since 2022-07-31
     */
    fun removeItem(position: Int) {
        if (position >= 0) {
            data.removeAt(position)
            notifyDataSetChanged()
        }
    }
}