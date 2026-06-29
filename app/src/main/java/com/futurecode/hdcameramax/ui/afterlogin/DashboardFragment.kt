package com.futurecode.hdcameramax.ui.afterlogin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentDashboardBinding
import com.futurecode.hdcameramax.model.MediaItem

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    private lateinit var viewModel: DashboardViewModel

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.updateCameraPermission(granted)
            if (granted) {
                navigateToCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            DashboardViewModel.Factory(
                DashboardRepository(requireContext().applicationContext)
            )
        )[DashboardViewModel::class.java]

        setupClickListeners()
        observeDashboard()
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            refreshDashboard()
        }
    }

    private fun setupClickListeners() {
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_settingsFragment)
        }

        binding.btnOpenCamera.setOnClickListener {
            openCamera()
        }

        binding.cvTakePhoto.setOnClickListener {
            openCamera()
        }

        binding.cvGallery.setOnClickListener {
            navigateToGallery()
        }

        binding.fabCapture.setOnClickListener {
            openCamera()
        }

        binding.tvSeeAllTools.setOnClickListener {
            openCamera()
        }

        binding.tvViewAllPhotos.setOnClickListener {
            navigateToGallery()
        }

        listOf(
            binding.ivRecentPhotoOne,
            binding.ivRecentPhotoTwo,
            binding.ivRecentPhotoThree,
            binding.ivRecentPhotoFour
        ).forEach { recentView ->
            recentView.setOnClickListener {
                val hasRecentPhoto = viewModel.uiState.value?.recentPhotos?.isNotEmpty() == true
                if (hasRecentPhoto) {
                    findNavController().navigate(R.id.action_dashboardFragment_to_photoAndVideoViewFragment)
                } else {
                    navigateToGallery()
                }
            }
        }
    }

    private fun openCamera() {
        if (hasCameraPermission()) {
            navigateToCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun observeDashboard() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            renderRecentPhotos(uiState.recentPhotos)
        }
    }

    private fun refreshDashboard() {
        viewModel.refreshDashboard(hasCameraPermission())
    }

    private fun renderRecentPhotos(recentPhotos: List<MediaItem>) {
        val slots = listOf(
            binding.ivRecentPhotoOne,
            binding.ivRecentPhotoTwo,
            binding.ivRecentPhotoThree,
            binding.ivRecentPhotoFour
        )

        slots.forEachIndexed { index, imageView ->
            val item = recentPhotos.getOrNull(index)
            if (item == null) {
                imageView.setImageResource(R.drawable.bg_dashboard_recent_placeholder)
            } else {
                Glide.with(this)
                    .load(item.uri)
                    .placeholder(R.drawable.bg_dashboard_recent_placeholder)
                    .centerCrop()
                    .into(imageView)
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun navigateToCamera() {
        findNavController().navigate(R.id.action_dashboardFragment_to_hdCameraFragment)
    }

    private fun navigateToGallery() {
        findNavController().navigate(R.id.action_dashboardFragment_to_galleryFragment)
    }

}
