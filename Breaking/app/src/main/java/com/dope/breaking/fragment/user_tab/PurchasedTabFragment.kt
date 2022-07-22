package com.dope.breaking.fragment.user_tab

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dope.breaking.R
import com.dope.breaking.databinding.FragmentPurchasedTabBinding


class PurchasedTabFragment : Fragment() {
    private lateinit var binding: FragmentPurchasedTabBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPurchasedTabBinding.inflate(inflater, container, false)
        return binding.root
    }
}