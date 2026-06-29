package com.futurecode.hdcameramax.base


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.utils.PrefManager

typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

abstract class BaseFragment<VB : ViewBinding>(
    private val inflate: Inflate<VB>
) : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: error("Binding is only valid between onCreateView and onDestroyView")

    protected val bindingOrNull: VB?
        get() = _binding

    protected val prefManager: PrefManager by lazy { MyApplication.app.prefManager }

    var distanceUnit = "km"
    var waterUnit = "ml"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected fun showAds(flNative: FrameLayout) {
        if (!isAdded) return
    }
}