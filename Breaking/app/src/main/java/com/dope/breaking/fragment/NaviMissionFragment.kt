package com.dope.breaking.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.dope.breaking.R

class NaviMissionFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_navi_mission, container, false)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        activity?.menuInflater?.inflate(R.menu.general_title_menu, menu)
    }
}