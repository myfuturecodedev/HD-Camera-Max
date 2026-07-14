package com.futurecode.hdcameramax.ui.afterlogin

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.ads.native_ad.NativeAdsHelper
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.DialogExitAppBinding
import com.futurecode.hdcameramax.databinding.FragmentDashboardBinding
import com.futurecode.hdcameramax.model.MediaItem
import com.futurecode.hdcameramax.utils.Utils.showRewardAdDialog

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    private lateinit var viewModel: DashboardViewModel
    private var pendingCameraFeature: String? = null
    private var exitDialog: Dialog? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.updateCameraPermission(granted)
            if (granted) {
                navigateToCamera()
            } else {
                pendingCameraFeature = null
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    private lateinit var nativeAdsHelper: NativeAdsHelper


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
        nativeAdsHelper= NativeAdsHelper(requireActivity())
        loadNativeAds()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog() // Trigger your popup here
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            refreshDashboard()
        }
    }

    override fun onDestroyView() {
        exitDialog?.dismiss()
        exitDialog = null
        super.onDestroyView()
    }

    private fun setupClickListeners() {
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_settingsFragment)
        }

        binding.btnOpenCamera.setOnClickListener {
            this@DashboardFragment.showRewardAdDialog(onRewardEarned = {
                // Ad khatam hote hi direct instant sound trigger
                openCamera()

            }) {
                Toast.makeText(requireContext(), "Ad failed to display", Toast.LENGTH_SHORT).show()
            }

        }

        binding.cvTakePhoto.setOnClickListener {
            this@DashboardFragment.showRewardAdDialog(onRewardEarned = {
                // Ad khatam hote hi direct instant sound trigger
                openCamera()

            }) {
                Toast.makeText(requireContext(), "Ad failed to display", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cvGallery.setOnClickListener {
            navigateToGallery()
        }

        binding.fabCapture.setOnClickListener {
            this@DashboardFragment.showRewardAdDialog(onRewardEarned = {
                // Ad khatam hote hi direct instant sound trigger
                openCamera()

            }) {
                Toast.makeText(requireContext(), "Ad failed to display", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvSeeAllTools.setOnClickListener {
            navigateToGallery()
        }

        binding.tvViewAllPhotos.setOnClickListener {
            navigateToGallery()
        }

        binding.featureHdZoom.setOnClickListener {
            this@DashboardFragment.showRewardAdDialog(onRewardEarned = {
                // Ad khatam hote hi direct instant sound trigger
                openCamera(HdCameraFragment.FEATURE_HD_ZOOM)


            }) {
                Toast.makeText(requireContext(), "Ad failed to display", Toast.LENGTH_SHORT).show()
            }
        }

        binding.featurePortrait.setOnClickListener {
            this@DashboardFragment.showRewardAdDialog(onRewardEarned = {
                // Ad khatam hote hi direct instant sound trigger
                openCamera(HdCameraFragment.FEATURE_PORTRAIT)


            }) {
                Toast.makeText(requireContext(), "Ad failed to display", Toast.LENGTH_SHORT).show()
            }
        }

        binding.featureFilters.setOnClickListener {
            this@DashboardFragment.showRewardAdDialog(onRewardEarned = {
                // Ad khatam hote hi direct instant sound trigger
                openCamera(HdCameraFragment.FEATURE_FILTERS)


            }) {
                Toast.makeText(requireContext(), "Ad failed to display", Toast.LENGTH_SHORT).show()
            }
        }

        binding.featureBeauty.setOnClickListener {
            this@DashboardFragment.showRewardAdDialog(onRewardEarned = {
                // Ad khatam hote hi direct instant sound trigger
                openCamera(HdCameraFragment.FEATURE_BEAUTY)

            }) {
                Toast.makeText(requireContext(), "Ad failed to display", Toast.LENGTH_SHORT).show()
            }

        }

        listOf(
            binding.ivRecentPhotoOne,
            binding.ivRecentPhotoTwo,
            binding.ivRecentPhotoThree,
            binding.ivRecentPhotoFour
        ).forEachIndexed { index, recentView ->
            recentView.setOnClickListener {
                val item = viewModel.uiState.value?.recentPhotos?.getOrNull(index)
                if (item != null) {
                    val args = Bundle().apply {
                        putString(PhotoAndVideoViewFragment.ARG_MEDIA_URI, item.uri.toString())
                        putBoolean(PhotoAndVideoViewFragment.ARG_IS_VIDEO, item.isVideo)
                        putLong(PhotoAndVideoViewFragment.ARG_DATE_ADDED, item.dateAdded)
                    }
                    findNavController().navigate(
                        R.id.action_dashboardFragment_to_photoAndVideoViewFragment,
                        args
                    )
                } else {
                    navigateToGallery()
                }
            }
        }
    }

    private fun openCamera() {
        openCamera(null)
    }

    private fun openCamera(feature: String?) {
        pendingCameraFeature = feature
        if (hasCameraPermission()) {
            navigateToCamera(feature)
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
        navigateToCamera(pendingCameraFeature)
        pendingCameraFeature = null
    }

    private fun navigateToCamera(feature: String?) {
        val args = Bundle().apply {
            feature?.let { putString(HdCameraFragment.ARG_DASHBOARD_FEATURE, it) }
        }
        findNavController().navigate(R.id.action_dashboardFragment_to_hdCameraFragment, args)
        pendingCameraFeature = null
    }

    private fun navigateToGallery() {
        findNavController().navigate(R.id.action_dashboardFragment_to_galleryFragment)
    }

    fun loadNativeAds() {
        activity?.let { currentActivity ->
            if (nativeAdsHelper == null) {
                nativeAdsHelper = NativeAdsHelper(currentActivity)
            }
            nativeAdsHelper?.showNativeAd(
                nativeBannerAdView = binding.nativeAds3.frame,
                mainLayout = binding.nativeAds3.mainLayout,
                placeholder = binding.nativeAds3.placeholder
            )
        }
    }

    private fun showExitConfirmationDialog() {
        if (exitDialog?.isShowing == true) return

        val dialogBinding = DialogExitAppBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setOnDismissListener { exitDialog = null }
        }

        dialogBinding.btnDismissCross.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnActionStay.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnActionConfirmExit.setOnClickListener {
            dialog.dismiss()
            requireActivity().finishAffinity()
        }

        exitDialog = dialog
        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

}
