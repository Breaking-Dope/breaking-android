package com.dope.breaking.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.dope.breaking.R
import com.dope.breaking.util.ValueUtil

class UserPostAdapter(private val context: Context, var data: MutableList<String>) :
    RecyclerView.Adapter<UserPostAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserPostAdapter.ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail = itemView.findViewById<ImageView>(R.id.imgv_post_thumbnail)
        fun bind(item: String) {
            Glide.with(itemView)
                .load(ValueUtil.IMAGE_BASE_URL + item)
                .placeholder(R.drawable.ic_default_post_image_size_up)
                .error(R.drawable.ic_default_post_image_size_up)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))

                .into(thumbnail)
        }
    }
}