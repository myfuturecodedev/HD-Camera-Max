package com.futurecode.hdcameramax.ui.prelogin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.ads.interstitial_ad.FullScreenAdsHelper
import com.futurecode.hdcameramax.ads.native_ad.NativeAdsHelper
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentPermissionBinding
import com.futurecode.hdcameramax.utils.Utils.setAdClickListener

//class PermissionFragment : BaseFragment<FragmentPermissionBinding>(FragmentPermissionBinding::inflate) {
//
//    // 1. Register permissions callback launcher engine
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
//        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
//
//        binding.switchPermission.isChecked = cameraGranted && audioGranted
//
//        if (cameraGranted && audioGranted) {
//            Toast.makeText(context, "Permissions Granted Successfully", Toast.LENGTH_SHORT).show()
//            // Jese hi popup me permission 'Allow' ho, instantly agle page par bhej do
//            navigateToHomeDashboard()
//        } else {
//            Toast.makeText(context, "Camera and Audio permissions are required to proceed.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Auto-bypass guard: Agar user pehle se permissions de chuka hai, toh screen load hote hi bypass kar do
//        if (hasPermissions()) {
//            navigateToHomeDashboard()
//            return
//        }
//
//        updateSwitchState()
//
//        // Switch control toggle implementation
//        binding.switchPermission.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked && !hasPermissions()) {
//                requestPermissions()
//            } else if (!isChecked && hasPermissions()) {
//                // Manual unchecking block karna taaki state UI sync me rahe
//                updateSwitchState()
//            }
//        }
//
//        // FIXED: Click listener routing logic (Exact requirement match)
//        binding.btnContinue.setOnClickListener {
//            if (hasPermissions()) {
//                // Agar already allowed h, direct agle page par bhejo
//                navigateToHomeDashboard()
//            } else {
//                // Agar allowed nahi h, pehle system popup show karo permissions dialog ka
//                requestPermissions()
//            }
//        }
//    }
//
//    private fun hasPermissions(): Boolean {
//        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
//        val audioPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
//        return cameraPermission == PackageManager.PERMISSION_GRANTED && audioPermission == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun updateSwitchState() {
//        binding.switchPermission.isChecked = hasPermissions()
//    }
//
//    private fun requestPermissions() {
//        requestPermissionLauncher.launch(
//            arrayOf(
//                Manifest.permission.CAMERA,
//                Manifest.permission.RECORD_AUDIO
//            )
//        )
//    }
//
//    // FIXED: Centralized single-line routing block helper logic using MainActivity interface handle
//    private fun navigateToHomeDashboard() {
//        prefManager.isOnboardingDone=true
//        if (!isAdded) return
//        findNavController().navigate(R.id.action_permissionFragment_to_welcomeFragment)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // Handle runtime app settings background modifications seamlessly
//        if (hasPermissions()) {
//            navigateToHomeDashboard()
//        } else {
//            updateSwitchState()
//        }
//    }
//}

class PermissionFragment : BaseFragment<FragmentPermissionBinding>(FragmentPermissionBinding::inflate) {

    private var shouldNavigateAfterPermissionGrant = false
    private var isUpdatingSwitchState = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        updateSwitchState()

        if (cameraGranted && audioGranted) {
            Toast.makeText(context, "Permissions Granted Successfully", Toast.LENGTH_SHORT).show()
            if (shouldNavigateAfterPermissionGrant) {
                navigateToHomeDashboard()
            }
        } else {
            Toast.makeText(context, "Camera and Audio permissions are required to proceed.", Toast.LENGTH_SHORT).show()
        }
        shouldNavigateAfterPermissionGrant = false
    }

    private var nativeAdsHelper: NativeAdsHelper? = null
    private var fullScreenAdsHelper: FullScreenAdsHelper? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nativeAdsHelper= NativeAdsHelper(requireActivity())
        fullScreenAdsHelper=FullScreenAdsHelper(requireActivity())

        updateSwitchState()
        loadNativeAds()

        binding.switchPermission.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchState) return@setOnCheckedChangeListener
            if (isChecked && !hasPermissions()) {
                requestPermissions(navigateAfterGrant = false)
            } else if (!isChecked && hasPermissions()) {
                updateSwitchState()
            }
        }


        fullScreenAdsHelper?.let { helper ->
            binding.btnContinue.setAdClickListener(
                activity = requireActivity(),
                adsHelper = helper, // ✅ FIXED: Safe non-null reference inside 'let' scope
                isShowEveryTime = false
            ) {
                if (hasPermissions()) {
                    navigateToHomeDashboard()
                } else {
                    requestPermissions(navigateAfterGrant = true)
                }
            }
        }

    }

    private fun hasPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
        return cameraPermission == PackageManager.PERMISSION_GRANTED && audioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun updateSwitchState() {
        isUpdatingSwitchState = true
        binding.switchPermission.isChecked = hasPermissions()
        isUpdatingSwitchState = false
    }

    private fun requestPermissions(navigateAfterGrant: Boolean) {
        shouldNavigateAfterPermissionGrant = navigateAfterGrant
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    // ====================================================================
    // 👑 FIXED: Added Current Destination Safeguard Guard Lock
    // ====================================================================
    private fun navigateToHomeDashboard() {
        prefManager.isOnboardingDone = true
        if (!isAdded) return

        val navController = findNavController()
        // ✅ SAFETY CHECK: Navigation tabhi karo jab current screen actual me permissionFragment ho
        if (navController.currentDestination?.id == R.id.permissionFragment) {
            navController.navigate(R.id.action_permissionFragment_to_welcomeFragment)
        }
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

    override fun onResume() {
        super.onResume()
        updateSwitchState()
    }
}
