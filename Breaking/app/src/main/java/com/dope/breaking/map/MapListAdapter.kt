package com.dope.breaking.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.R
import com.dope.breaking.model.LocationList

class MapListAdapter(val itemList: ArrayList<LocationList>): RecyclerView.Adapter<MapListAdapter.ViewHolder>() {
    
    private lateinit var itemListClickListener : OnItemClickListener // 아이템 리스트 클릭 리스너
    private lateinit var itemButtonClickListener : OnItemClickListener // 아이템 리스트 버튼 클릭 리스너
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_location_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = itemList[position].name
        holder.road.text = itemList[position].road
        holder.address.text = itemList[position].address
        // 아이템 리스트 클릭 이벤트
        holder.itemView.setOnClickListener {
            itemListClickListener.onClick(it, position)
        }
        // 아이템 선택 버튼 클릭 이벤트
        holder.selectButton.setOnClickListener {
            itemButtonClickListener.onClick(it, position)
        }
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_list_name)
        val road: TextView = itemView.findViewById(R.id.tv_list_road)
        val address: TextView = itemView.findViewById(R.id.tv_list_address)
        val selectButton: Button = itemView.findViewById(R.id.btn_select)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    // 액티비티에서 클릭 이벤트 오버라이드 하기 위해 인터페이스 정의
    interface OnItemClickListener {
        fun onClick(v: View, position: Int)
    }

    // 아이템 리스트 클릭 리스너 함수
    fun setItemListClickListener(onItemClickListener: OnItemClickListener) {
        this.itemListClickListener = onItemClickListener // 액티비티에서 구현한 인터페이스 정보를 할당
    }

    // 아이템 선택 버튼 클릭 리스너 함수
    fun setItemButtonClickListener(onItemClickListener: OnItemClickListener) {
        this.itemButtonClickListener = onItemClickListener // 액티비티에서 구현한 인터페이스 정보를 할당
    }
    
}