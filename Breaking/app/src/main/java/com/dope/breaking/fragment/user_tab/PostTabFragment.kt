package com.dope.breaking.fragment.user_tab

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import com.dope.breaking.R
import com.dope.breaking.adapter.UserPostAdapter
import com.dope.breaking.databinding.FragmentPostTabBinding

class PostTabFragment : Fragment() {
    private lateinit var binding: FragmentPostTabBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostTabBinding.inflate(inflater, container, false)
        var data = mutableListOf<String>()
        data.add("/static/compressedProfileImg/d6a7cc89-4874-4298-be1b-48e3e63ed66d.jpg")
        data.add("/static/compressedProfileImg/d6a2f759-2af3-4645-a3d9-09034eec6c33.jpg")

        val adapter = UserPostAdapter(requireActivity(), data)
        binding.rcvUserPost.addItemDecoration(
            DividerItemDecoration(
                requireActivity(),
                LinearLayout.VERTICAL
            )
        )
        binding.rcvUserPost.adapter = adapter
        return binding.root
    }
}