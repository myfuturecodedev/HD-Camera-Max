package com.futurecode.hdcameramax.ui.afterlogin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.ads.interstitial_ad.FullScreenAdsHelper
import com.futurecode.hdcameramax.ads.native_ad.NativeAdsHelper
import com.futurecode.hdcameramax.base.BaseFragment
import com.futurecode.hdcameramax.databinding.FragmentCameraFeaturesBinding


class CameraFeaturesFragment : BaseFragment<FragmentCameraFeaturesBinding>(FragmentCameraFeaturesBinding::inflate) {
    private var nativeAdsHelper: NativeAdsHelper? = null
    private var fullScreenAdsHelper: FullScreenAdsHelper? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nativeAdsHelper= NativeAdsHelper(requireActivity())
        fullScreenAdsHelper= FullScreenAdsHelper(requireActivity())

    }
}