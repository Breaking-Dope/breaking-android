package com.dope.breaking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dope.breaking.util.ValueUtil

/**
 * 이미지 확대 클래스
 */
class ImageExpansionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_expansion)

        if (intent != null) {
            val imgUrl = intent.getStringExtra("imgUrl") // 받아온 이미지 url 가져오기

            Glide.with(this)
                .load(ValueUtil.IMAGE_BASE_URL + imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_default_profile_image)
                .into(findViewById(R.id.full_image))
        }
        /*
        이미지 클릭 시, 혹은 뒤로 가기 버튼 클릭 시 애니메이션 효과와 함께 뒤로 가기
         */
        findViewById<ImageView>(R.id.full_image).setOnClickListener {
            supportFinishAfterTransition()
        }
        findViewById<ImageButton>(R.id.img_btn_back).setOnClickListener {
            supportFinishAfterTransition()
        }
    }
}