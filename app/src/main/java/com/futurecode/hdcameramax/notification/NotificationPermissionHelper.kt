package com.futurecode.hdcameramax.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.futurecode.hdcameramax.activity.MyApplication


class NotificationPermissionHelper(private val fragment: Fragment) {

    private val requestPermissionLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                val safeContext = fragment.requireContext()
                NotificationScheduler.startNotificationWorker(safeContext)
                MyApplication.app.prefManager.isNotificationStarts = true

                Log.d("NotificationHelper", "Notification permission GRANTED by user.")
            } else {
                Log.d("NotificationHelper", "Notification permission DENIED by user.")
            }
        }

    // ✅ FIXED: Added isRefresh flag boundary check to avoid duplicate background loops
    fun checkAndRequestPermission(isRefresh: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val safeContext = fragment.requireContext()

            when {
                ContextCompat.checkSelfPermission(
                    safeContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("NotificationHelper", "Permission already granted.")

                    // ⚠️ CRITICAL CONTROL: Only execute worker trigger if it's first app session initialization loop
                    if (!isRefresh) {
                        NotificationScheduler.startNotificationWorker(safeContext)
                        MyApplication.app.prefManager.isNotificationStarts = true
                    } else {
                        Log.d("NotificationHelper", "Skipping worker trigger chain. Session already operational.")
                    }
                }
                fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("NotificationHelper", "Android < 13 detected. Executing direct session check workflows.")
            if (!isRefresh) {
                NotificationScheduler.startNotificationWorker(fragment.requireContext())
                MyApplication.app.prefManager.isNotificationStarts = true
            }
        }
    }
}