package com.dope.breaking.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.dope.breaking.databinding.FragmentNaviChatBinding

class NaviChatFragment : Fragment() {
    private lateinit var binding: FragmentNaviChatBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNaviChatBinding.inflate(inflater, container, false)

        return binding.root
    }
}