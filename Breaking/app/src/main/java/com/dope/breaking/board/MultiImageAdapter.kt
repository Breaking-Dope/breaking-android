package com.dope.breaking.board

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dope.breaking.R
import com.dope.breaking.databinding.ActivityPostBinding

class MultiImageAdapter(var list: ArrayList<Uri>, var fileNameList:ArrayList<String>, var postBitmapList:ArrayList<Bitmap>, var context: Context, var binding: ActivityPostBinding) : RecyclerView.Adapter<MultiImageAdapter.ViewHolder>() {

    class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!){
        // 데이터가 바인딩 당할 Item XML 내부의 elements 들 정의
        val ivPostImage = view?.findViewById<ImageView>(R.id.iv_post_image)
        val ibCancel = view?.findViewById<ImageButton>(R.id.ib_cancel)

        fun bind(uri : Uri, context : Context){
            // 글라이드로 이미지 보여주기
            Glide.with(context)
                .asBitmap()
                .load(uri)
                .into(ivPostImage!!)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.multi_image_item, parent, false) // 바인딩 당할 Item XML 파일명 지정
        return ViewHolder(view)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        binding.tvCurrentCountImage.setText(itemCount.toString()) // 지속적으로 이미지 개수 카운팅해서 보여주기
        if (itemCount < 20){ // 최대 개수가 아니면 텍스트 색상 복구
            binding.tvCurrentCountImage.setTextColor(R.color.post_upload_count_color)
            binding.tvMiddleCountImage.setTextColor(R.color.post_upload_count_color)
            binding.tvTotalCountImage.setTextColor(R.color.post_upload_count_color)
        }
        // 데이터를 순서대로 바인딩
        holder.bind(list[position], context)
        holder.ibCancel?.setOnClickListener{ // 해당 포지션 이미지의 삭제 버튼을 누른다면
           removeItem(position) // 해당 포지션의 이미지 삭제하고
           binding.tvCurrentCountImage.setText(itemCount.toString()) // 이미지 개수 카운팅해서 보여주기
        }
    }

    override fun getItemCount(): Int {
        return list.size // 어댑터로 바인딩된 아이템 개수 반환
    }

    fun removeItem(position: Int){
        if(position >= 0){
            list.removeAt(position)
            fileNameList.removeAt(position)
            postBitmapList.removeAt(position)
            notifyDataSetChanged() // 변경된 아이템 갱신 처리
        }
    }
}