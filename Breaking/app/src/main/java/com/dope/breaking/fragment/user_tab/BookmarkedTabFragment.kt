package com.dope.breaking.fragment.user_tab

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dope.breaking.databinding.FragmentBookmarkedTabBinding

class BookmarkedTabFragment : Fragment() {
    private lateinit var binding: FragmentBookmarkedTabBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookmarkedTabBinding.inflate(inflater, container, false)
        return binding.root
    }
}