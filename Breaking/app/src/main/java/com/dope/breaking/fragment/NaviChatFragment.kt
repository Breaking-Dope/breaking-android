package com.dope.breaking.fragment

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.bumptech.glide.Glide
import com.dope.breaking.R
import com.dope.breaking.adapter.UserPostAdapter
import com.dope.breaking.databinding.FragmentNaviChatBinding
import com.dope.breaking.databinding.ItemPostListBinding
import com.dope.breaking.util.ValueUtil

class NaviChatFragment : Fragment() {
    private lateinit var binding: FragmentNaviChatBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNaviChatBinding.inflate(inflater, container, false)
        var data = mutableListOf<String>()
        data.add("/static/compressedProfileImg/d6a7cc89-4874-4298-be1b-48e3e63ed66d.jpg")
        data.add("/static/compressedProfileImg/d6a7cc89-4874-4298-be1b-48e3e63ed66d.jpg")

        val adapter = UserPostAdapter(requireActivity(), data)
        binding.rcv.addItemDecoration(
            DividerItemDecoration(
                requireActivity(),
                LinearLayout.VERTICAL
            )
        )
        binding.rcv.adapter = adapter
        return binding.root
    }
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        activity?.menuInflater?.inflate(R.menu.general_title_menu, menu)
//    }
}