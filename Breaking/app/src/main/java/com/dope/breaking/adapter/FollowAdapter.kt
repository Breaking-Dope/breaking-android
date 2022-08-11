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
import com.dope.breaking.follow.Follow
import com.dope.breaking.model.FollowData
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.user.UserProfile
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        // 리스트 상에서 본인이 들어가 있는 경우 버튼 hidden
        holder.removeButton.visibility =
            if (data[position].userId == ResponseExistLogin.baseUserInfo?.userId) View.GONE else View.VISIBLE

        val token =
            ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(context).getTokenFromLocal() // 요청 토큰

        /*
        로그인 & 비로그인 유저 구분 / 내 팔로우(워) 리스트 & 다른 유저 팔로우(워) 리스트 구분 / 팔로우 & 팔로워 상태 구분
         */
        if (ResponseExistLogin.baseUserInfo != null && JwtTokenUtil(context).getTokenFromLocal() != "") { // 로그인한 유저라면
            if (currentUserId == ResponseExistLogin.baseUserInfo?.userId) { // 내 팔로우(워) 리스트인 경우
                if (state) { // 팔로우 리스트
                    convertToFollowingState(holder.removeButton) // 팔로잉 버튼 상태로 전환
                    // 우측 버튼 클릭 이벤트
                    holder.removeButton.setOnClickListener {
                        // 다이얼로그
                        DialogUtil().MultipleDialog(
                            context,
                            "정말 '${data[position].nickname}'님의 팔로우를 취소하시겠습니까?",
                            "예",
                            "아니오",
                            {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result =
                                        Follow().startUnFollowRequest(
                                            token,
                                            data[position].userId
                                        ) // 언팔로우 요청
                                    if (result) { // 언팔로우 요청 성공 시
                                        removeItem(position) // 아이템 제거
                                    } else
                                        dialogRequestFail()
                                }
                            }).show()
                    }
                } else { // 팔로워 리스트
                    if (data[position].isFollowing) // 팔로우 중이라면
                        convertToFollowingState(holder.removeButton) // 팔로잉 버튼 상태로 전환
                    else // 팔로우 중이 아니라면
                        convertToFollowState(holder.removeButton) // 팔로우 버튼 상태로 전환

                    // 우측 버튼 클릭 이벤트
                    holder.removeButton.setOnClickListener {
                        clickFollowStateButton(position, token, holder)
                    }
                }
            } else { // 다른 사람 리스트인 경우
                if (data[position].isFollowing) // 팔로우 중이라면
                    convertToFollowingState(holder.removeButton) // 팔로잉 버튼 상태로 전환
                else // 팔로우 중이 아니라면
                    convertToFollowState(holder.removeButton) // 팔로우 버튼 상태로 전환

                // 우측 버튼 클릭 이벤트
                holder.removeButton.setOnClickListener {
                    clickFollowStateButton(position, token, holder)
                }
            }
        } else {
            holder.removeButton.visibility = View.GONE // 비로그인 유저의 경우 버튼 모두 숨김
        }
    }

    /**
     * 리스트에서 각 사람과의 팔로우관계에 따른 클릭 이벤트 (팔로잉 & 팔로우 버튼)
     * @param position(Int): 현재 리스트에서 클릭한 위치
     * @param token(String): Jwt 토큰
     * @param holder(ViewHolder): view holder
     * @author Seunggun Sin
     * @since 2022-08-05
     */
    private fun clickFollowStateButton(
        position: Int,
        token: String,
        holder: ViewHolder
    ) {
        if (data[position].isFollowing) { // 팔로우 중이라면
            DialogUtil().MultipleDialog(
                context,
                "정말 '${data[position].nickname}'님의 팔로우를 취소하시겠습니까?",
                "예",
                "아니오",
                {
                    CoroutineScope(Dispatchers.Main).launch {
                        val result =
                            Follow().startUnFollowRequest(
                                token,
                                data[position].userId
                            ) // 언팔로우 요청
                        if (result) { // 언팔로우 요청 성공 시
                            convertToFollowState(holder.removeButton) // 버튼 상태 전환
                            data[position].isFollowing = false // 팔로잉 상태로 전환
                        } else
                            dialogRequestFail() // 실패에 대한 다이얼로그
                    }
                }).show()
        } else { // 팔로우 중이 아니라면
            CoroutineScope(Dispatchers.Main).launch {
                val result =
                    Follow().startFollowRequest(token, data[position].userId) // 팔로우 요청
                if (result) { // 팔로우 요청 성공 시
                    convertToFollowingState(holder.removeButton) // 버튼 상태 전환
                    data[position].isFollowing = true // 팔로우 상태로 전환
                } else
                    dialogRequestFail() // 실패에 대한 다이얼로그
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNickname: TextView = itemView.findViewById(R.id.tv_following_nickname)
        private val textStatus: TextView = itemView.findViewById(R.id.tv_following_status)
        val imageProfile: ImageView = itemView.findViewById(R.id.imgv_following_profile)
        val removeButton: Button = itemView.findViewById(R.id.btn_following_remove)

        fun bind(item: FollowData) {
            textNickname.text = item.nickname
            textStatus.text = item.statusMsg
            Glide.with(itemView)
                .load(ValueUtil.IMAGE_BASE_URL + item.profileImgURL)
                .circleCrop()
                .error(R.drawable.ic_default_profile_image)
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

    /**
     * "팔로우" 버튼 상태로 전환(텍스트, 배경 변경)
     * @param button(Button): 우측 버튼 view
     * @author Seunggun Sin
     * @since 2022-08-02 | 2022-08-09
     */
    private fun convertToFollowState(button: Button) {
        button.text = "팔로우"
        button.setBackgroundResource(R.drawable.sign_up_user_type_unselected)
    }

    /**
     * "팔로잉" 버튼 상태로 전환(텍스트, 배경 변경)
     * @param button(Button): 우측 버튼 view
     * @author Seunggun Sin
     * @since 2022-08-02
     */
    private fun convertToFollowingState(button: Button) {
        button.text = "팔로잉"
        button.setBackgroundResource(R.drawable.feed_category_button_background)
    }

    /**
     * 팔로우, 언팔로우 요청 실패시 띄우는 dialog
     * @author Seunggun Sin
     * @since 2022-08-02
     */
    private fun dialogRequestFail() {
        DialogUtil().SingleDialog(context, "요청에 실패하였습니다. 재시도 바랍니다.", "확인").show()
    }
}