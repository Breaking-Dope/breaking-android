package com.dope.breaking.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dope.breaking.R
import com.dope.breaking.follow.Follow
import com.dope.breaking.model.response.ResponseExistLogin
import com.dope.breaking.model.response.ResponseUserSearch
import com.dope.breaking.user.UserProfile
import com.dope.breaking.util.DialogUtil
import com.dope.breaking.util.JwtTokenUtil
import com.dope.breaking.util.ValueUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserSearchAdapter(
    private val context: Context, // 컨텍스트
    var data: MutableList<ResponseUserSearch?> // 리스트
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ValueUtil.VIEW_TYPE_ITEM) {
            val view =
                LayoutInflater.from(context).inflate(R.layout.item_user_search, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is UserSearchAdapter.ItemViewHolder) {
            holder.bind(data[position]!!) // view 바인딩

            // 닉네임 클릭 시
            holder.textNickname.setOnClickListener {
                UserProfile(context as Activity).moveToUserPage(data[position]!!.userId)
            }
            // 프로필 이미지 클릭 시
            holder.imageProfile.setOnClickListener {
                UserProfile(context as Activity).moveToUserPage(data[position]!!.userId)
            }
            // 리스트 상에서 본인이 들어가 있는 경우 버튼 hidden
            holder.removeButton.visibility =
                if (data[position]!!.userId == ResponseExistLogin.baseUserInfo?.userId) View.GONE else View.VISIBLE

            val token =
                ValueUtil.JWT_REQUEST_PREFIX + JwtTokenUtil(context).getAccessTokenFromLocal() // 요청 토큰

            // 로그인, 비로그인 구분
            if (ResponseExistLogin.baseUserInfo != null && JwtTokenUtil(context).getAccessTokenFromLocal() != "") {
                if (data[position]!!.userId == ResponseExistLogin.baseUserInfo?.userId) { // 현재 아이템이 본인이라면
                    holder.removeButton.visibility = View.GONE // 우측 버튼 숨김
                } else { // 아니라면
                    if (data[position]!!.isFollowing) { // 내가 팔로우한 관계라면
                        convertToFollowingState(holder.removeButton) // 팔로잉 view 처리
                    } else { // 아니라면
                        convertToFollowState(holder.removeButton) // 팔로우 view 처리
                    }
                    // 우측 버튼 클릭 이벤트
                    holder.removeButton.setOnClickListener {
                        clickFollowStateButton(position, token, holder)
                    }
                }
            } else // 비로그인 유저의 경우
                holder.removeButton.visibility = View.GONE // 우측 버튼 숨김
        }
    }

    /**
     * 리스트에서 각 사람과의 팔로우관계에 따른 클릭 이벤트 (팔로잉 & 팔로우 버튼)
     * @param position(Int): 현재 리스트에서 클릭한 위치
     * @param token(String): Jwt 토큰
     * @param holder(ViewHolder): view holder
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    private fun clickFollowStateButton(
        position: Int,
        token: String,
        holder: UserSearchAdapter.ItemViewHolder
    ) {
        if (data[position]!!.isFollowing) { // 팔로우 중이라면
            DialogUtil().MultipleDialog(
                context,
                "정말 '${data[position]!!.nickname}'님의 팔로우를 취소하시겠습니까?",
                "예",
                "아니오",
                {
                    CoroutineScope(Dispatchers.Main).launch {
                        val result =
                            Follow().startUnFollowRequest(
                                token,
                                data[position]!!.userId
                            ) // 언팔로우 요청
                        if (result) { // 언팔로우 요청 성공 시
                            convertToFollowState(holder.removeButton) // 버튼 상태 전환
                            data[position]!!.isFollowing = false // 팔로잉 상태로 전환
                        } else
                            dialogRequestFail() // 실패에 대한 다이얼로그
                    }
                }).show()
        } else { // 팔로우 중이 아니라면
            CoroutineScope(Dispatchers.Main).launch {
                val result =
                    Follow().startFollowRequest(token, data[position]!!.userId) // 팔로우 요청
                if (result) { // 팔로우 요청 성공 시
                    convertToFollowingState(holder.removeButton) // 버튼 상태 전환
                    data[position]!!.isFollowing = true // 팔로우 상태로 전환
                } else
                    dialogRequestFail() // 실패에 대한 다이얼로그
            }
        }
    }

    /**
     * "팔로우" 버튼 상태로 전환(텍스트, 배경 변경)
     * @param button(Button): 우측 버튼 view
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    private fun convertToFollowState(button: Button) {
        button.text = "팔로우"
        button.setBackgroundResource(R.drawable.sign_up_user_type_unselected)
    }

    /**
     * "팔로잉" 버튼 상태로 전환(텍스트, 배경 변경)
     * @param button(Button): 우측 버튼 view
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    private fun convertToFollowingState(button: Button) {
        button.text = "팔로잉"
        button.setBackgroundResource(R.drawable.feed_category_button_background)
    }

    /**
     * 팔로우, 언팔로우 요청 실패시 띄우는 dialog
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    private fun dialogRequestFail() {
        DialogUtil().SingleDialog(context, "요청에 실패하였습니다. 재시도 바랍니다.", "확인").show()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position] == null) ValueUtil.VIEW_TYPE_LOADING else ValueUtil.VIEW_TYPE_ITEM
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNickname: TextView = itemView.findViewById(R.id.tv_user_search_nickname)
        private val textStatus: TextView = itemView.findViewById(R.id.tv_user_search_status)
        val imageProfile: ImageView = itemView.findViewById(R.id.imgv_user_search_profile)
        val removeButton: Button = itemView.findViewById(R.id.btn_user_search_remove)
        val email: TextView = itemView.findViewById(R.id.tv_user_search_email)
        val follower: TextView = itemView.findViewById(R.id.tv_user_search_follower)

        fun bind(item: ResponseUserSearch) {
            textNickname.text = item.nickname
            textStatus.text = item.statusMsg
            email.text = item.email
            follower.text = "팔로워 ${item.followerCount}명"

            if (item.profileImgURL != null)
                Glide.with(itemView)
                    .load(ValueUtil.IMAGE_BASE_URL + item.profileImgURL)
                    .placeholder(R.drawable.ic_default_profile_image)
                    .circleCrop()
                    .error(R.drawable.ic_default_profile_image)
                    .into(imageProfile)
            else
                Glide.with(itemView)
                    .load(R.drawable.ic_default_profile_image)
                    .circleCrop()
                    .into(imageProfile)
        }

    }

    inner class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val progressDialog = itemView.findViewById<ProgressBar>(R.id.progressbar_loading)
    }

    /**
     * 응답으로 받아온 리스트를 리스트에 추가하는 함수
     * @param items(List<FollowData>): 팔로우 데이터 리스트
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    fun addItems(items: List<ResponseUserSearch>) {
        data.addAll(items)
        notifyItemRangeInserted(itemCount, items.size)
    }

    /**
     * 응답으로 받아온 데이터를 리스트에 추가하는 함수
     * @param item(FollowData): 팔로우 데이터
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    fun addItem(item: ResponseUserSearch?) {
        data.add(item)
        notifyItemInserted(itemCount)
    }

    /**
     * 리스트의 마지막 아이템을 지우는 함수
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    fun removeLast() {
        data.removeAt(data.size - 1)
        notifyItemRemoved(data.size)
    }

    /**
     * 현재 아이템을 새로운 아이템으로 대체하는 함수
     * @author Seunggun Sin
     * @since 2022-08-31
     */
    fun replaceAll(items: List<ResponseUserSearch>) {
        data.clear()
        data.addAll(items)
        notifyDataSetChanged()
    }

}