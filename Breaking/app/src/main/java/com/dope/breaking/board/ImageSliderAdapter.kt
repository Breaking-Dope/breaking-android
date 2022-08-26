package com.dope.breaking.board

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.dope.breaking.R
import com.dope.breaking.util.ValueUtil

class ImageSliderAdapter(val context: Context, var sliderImage: ArrayList<String?>) : RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    // 아이템이 정상적으로 존재한다면 보여줄 바인딩 function 정의
    class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!){
        val mImageView = view?.findViewById<ImageView>(R.id.image_slider)

        fun bindSliderImage(imageURL: String?, context: Context){
            Log.d("ImageSliderAdapter.kt", "이미지 URL 테스트: "+ValueUtil.IMAGE_BASE_URL + imageURL)
            Glide.with(context)
                .load(ValueUtil.IMAGE_BASE_URL + imageURL)
                .apply(RequestOptions().transform(CenterCrop()))
                .error(R.drawable.ic_default_post_image_size_up)
                .into(mImageView!!)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_slider, parent, false) // 바인딩 당할 Item XML 파일명 지정
        return ViewHolder(view) // 아이템이 있다면 ViewHolder 인스턴스 리턴
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindSliderImage(sliderImage[position], context)
    }

    override fun getItemCount(): Int {
        return sliderImage.size
    }

}