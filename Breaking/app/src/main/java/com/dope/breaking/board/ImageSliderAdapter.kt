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

class ImageSliderAdapter(val context: Context, var sliderMedia: ArrayList<String?>) :
    RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    private var simpleExoPlayer: SimpleExoPlayer? = null // Exoplayer2 변수
    private var simpleExoPlayerList =
        ArrayList<SimpleExoPlayer>() // Exoplayer2 영상 자원(들)을 멈추고, 관리하기 위해 모아두는 리스트

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val mImageView = view?.findViewById<ImageView>(R.id.image_slider)
        var videoView = view?.findViewById<PlayerView>(R.id.video_player_view)

        fun bindSlider(mediaURL: String?, context: Context) {
            Log.d("ImageSliderAdapter.kt", "받아온 미디어 URL : " + ValueUtil.IMAGE_BASE_URL + mediaURL)

            if (mediaURL!!.split(".")[1] == "mp4") { // 영상이면
                videoView?.visibility = View.VISIBLE // 비디오뷰 나타내기
                mImageView?.visibility = View.GONE // 이미지 숨기기

                simpleExoPlayer = SimpleExoPlayer.Builder(context).build()
                simpleExoPlayerList.add(simpleExoPlayer!!)

                videoView?.player = simpleExoPlayer
                simpleExoPlayer!!.addMediaItem(MediaItem.fromUri(Uri.parse(ValueUtil.IMAGE_BASE_URL + mediaURL)))
                simpleExoPlayer!!.prepare()
                simpleExoPlayer!!.playWhenReady = false // 자동 재생은 false
            } else { // 이미지면
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
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_slider, parent, false) // 바인딩 할 Item XML 파일명 지정
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindSlider(sliderMedia[position], context)
    }

    override fun getItemCount(): Int {
        return sliderMedia.size
    }

    /**
     * 응답으로 받아온 미디어 리스트를 추가하는 함수
     * @param items(ArrayList<String>): 미디어 URL 리스트
     * @author Tae hyun Park
     * @since 2022-09-09
     */
    fun addItems(items: ArrayList<String?>) {
        sliderMedia.addAll(items)
        notifyItemRangeInserted(itemCount, items.size)
    }

    /**
     * ExoPlayer 자원을 해제하는 함수로, 메모리 누수(Memory leak)을 방지하기 위한 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-09
     */
    fun onDetach() {
        for (i in 0 until simpleExoPlayerList.size) {
            simpleExoPlayerList[i].release()
        }
    }

    /**
     * ExoPlayer 자원을 일시 멈춤하는 함수로, 앱 밖의 다른 화면으로 갔다가 다시 돌아올 때 영상을 다시 볼 수 있도록 하는 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-09-09
     */
    fun onPause() {
        for (i in 0 until simpleExoPlayerList.size) {
            simpleExoPlayerList[i].pause()
        }
    }

}