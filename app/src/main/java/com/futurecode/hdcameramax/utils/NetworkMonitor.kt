package com.futurecode.hdcameramax.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.databinding.DialogNoInterneBinding
import kotlin.div
import kotlin.times


class NetworkMonitor(private val applicationContext: Context) {

    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var noInternetDialog: BaseDialog? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val noInternetDelayMillis = 20_000L
    private var isNoInternetDialogScheduled = false

    private val delayedNoInternetRunnable = Runnable {
        isNoInternetDialogScheduled = false
        if (!isInternetActuallyAvailable()) {
            showNoInternetDialog()
        }
    }

    fun startMonitoring() {
        if (networkCallback != null) return

        networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                handleInternetAvailable()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                handleInternetLost()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                val isValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                Log.d("NetworkMonitor", "Network validated = $isValidated")

                if (isValidated) {
                    handleInternetAvailable()
                } else {
                    handleInternetLost()
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                Log.d("NetworkMonitor", "Network unavailable")
                handleInternetLost()
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback!!)

        checkInitialInternetState()
    }

    fun stopMonitoring() {
        try {
            cancelPendingNoInternetDialog()
            closeNoInternetDialog()

            networkCallback?.let {
                connectivityManager.unregisterNetworkCallback(it)
            }
            networkCallback = null
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "stopMonitoring exception: $e")
        }
    }

    private fun checkInitialInternetState() {
        if (isInternetActuallyAvailable()) {
        } else {
            Log.d("NetworkMonitor", "Initial state: internet not available")
            handleInternetLost()
        }
    }

    private fun handleInternetAvailable() {
        cancelPendingNoInternetDialog()
        closeNoInternetDialog()
    }

    private fun handleInternetLost() {
        if (noInternetDialog?.isShowing == true) return
        scheduleNoInternetDialog()
    }

    private fun scheduleNoInternetDialog() {
        if (isNoInternetDialogScheduled) return

        isNoInternetDialogScheduled = true
        mainHandler.postDelayed(delayedNoInternetRunnable, noInternetDelayMillis)
    }

    private fun cancelPendingNoInternetDialog() {
        mainHandler.removeCallbacks(delayedNoInternetRunnable)
        isNoInternetDialogScheduled = false
    }




    private fun isInternetActuallyAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showNoInternetDialog() {
        try {
            if (noInternetDialog?.isShowing == true) return

            val activity = (applicationContext as MyApplication).getCurrentActivity()

            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                activity.runOnUiThread {
                    if (noInternetDialog?.isShowing == true) return@runOnUiThread

                    noInternetDialog = BaseDialog(activity, R.style.TransparentDialog)
                    val binding = DialogNoInterneBinding.inflate(LayoutInflater.from(activity))

                    noInternetDialog?.setContentView(binding.root)
                    noInternetDialog?.setCancelable(false)
                    noInternetDialog?.window?.setLayout(
                        Utils.getWidth(activity) / 100 * 95,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    noInternetDialog?.window?.setGravity(Gravity.CENTER)
                    noInternetDialog?.show()
                }
            } else {
                Log.e("NetworkMonitor", "No active activity context available for dialog.")
            }
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "showNoInternetDialog exception: $e")
        }
    }

    private fun closeNoInternetDialog() {
        try {
            val activity = (applicationContext as MyApplication).getCurrentActivity()
            activity?.runOnUiThread {
                noInternetDialog?.dismiss()
                noInternetDialog = null
            }
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "closeNoInternetDialog exception: $e")
        }
    }
}