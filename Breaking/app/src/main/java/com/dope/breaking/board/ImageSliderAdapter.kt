package com.dope.breaking.board

import android.content.Context
import android.net.Uri
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
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView

class ImageSliderAdapter(val context: Context, var sliderMedia: ArrayList<String?>) : RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    // 아이템이 정상적으로 존재한다면 보여줄 바인딩 function 정의
    class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!){
        val mImageView = view?.findViewById<ImageView>(R.id.image_slider)
        val videoView = view?.findViewById<PlayerView>(R.id.video_player_view)

        fun bindSlider(mediaURL: String?, context: Context){
            Log.d("ImageSliderAdapter.kt", "받아온 미디어 URL : "+ValueUtil.IMAGE_BASE_URL + mediaURL)

            if (mediaURL!!.split(".")[1] == "mp4"){ // 영상이면
                videoView?.visibility = View.VISIBLE // 비디오뷰 나타내기
                mImageView?.visibility = View.GONE // 이미지 숨기기
                val simpleExoPlayer = SimpleExoPlayer.Builder(context).build()
                videoView?.player = simpleExoPlayer
                simpleExoPlayer.addMediaItem(MediaItem.fromUri(Uri.parse(ValueUtil.IMAGE_BASE_URL + mediaURL)))
                simpleExoPlayer.prepare()
                simpleExoPlayer.playWhenReady = false // 자동 재생 false
            }else{ // 이미지면
                videoView?.visibility = View.GONE // 비디오뷰 숨기기
                mImageView?.visibility = View.VISIBLE // 이미지 나타나기
                Glide.with(context)
                    .load(ValueUtil.IMAGE_BASE_URL + mediaURL)
                    .apply(RequestOptions().transform(CenterCrop()))
                    .error(R.drawable.ic_default_post_image_size_up)
                    .into(mImageView!!)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_slider, parent, false) // 바인딩 당할 Item XML 파일명 지정
        return ViewHolder(view) // 아이템이 있다면 ViewHolder 인스턴스 리턴
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindSlider(sliderMedia[position], context)
    }

    override fun getItemCount(): Int {
        return sliderMedia.size
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.videoView?.player?.release()
    }

}