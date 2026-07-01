package com.futurecode.hdcameramax.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.futurecode.hdcameramax.databinding.FragmentFavouriteBinding

class FavouriteFragment : Fragment() {

    private var _binding: FragmentFavouriteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavouriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRecyclerViews()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerViews() {
        // Today section - 3 columns, 6 items
        val todayItems = listOf(
            FavouriteAdapter.FavouriteItem(isVideo = false, hasBorder = true),
            FavouriteAdapter.FavouriteItem(isVideo = false),
            FavouriteAdapter.FavouriteItem(isVideo = true, duration = "00:15"),
            FavouriteAdapter.FavouriteItem(isVideo = false),
            FavouriteAdapter.FavouriteItem(isVideo = false),
            FavouriteAdapter.FavouriteItem(isVideo = true, duration = "00:32")
        )
        binding.rvToday.adapter = FavouriteAdapter(todayItems)

        // Yesterday section - specific layout with spans
        val yesterdayItems = listOf(
            FavouriteAdapter.FavouriteItem(isVideo = false),
            FavouriteAdapter.FavouriteItem(isVideo = false),
            FavouriteAdapter.FavouriteItem(isVideo = false),
            FavouriteAdapter.FavouriteItem(isVideo = true, duration = "01:24"),
            FavouriteAdapter.FavouriteItem(isVideo = false)
        )
        
        val yesterdayLayoutManager = GridLayoutManager(requireContext(), 3)
        yesterdayLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 3) 2 else 1 // 4th item spans 2 columns
            }
        }
        binding.rvYesterday.layoutManager = yesterdayLayoutManager
        binding.rvYesterday.adapter = FavouriteAdapter(yesterdayItems)

        // Last Week section - 3 columns, 6 items
        val lastWeekItems = List(6) { FavouriteAdapter.FavouriteItem(isVideo = false) }
        binding.rvLastWeek.adapter = FavouriteAdapter(lastWeekItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = FavouriteFragment()
    }
}
