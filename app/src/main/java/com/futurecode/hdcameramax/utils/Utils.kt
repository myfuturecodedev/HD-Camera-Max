package com.futurecode.hdcameramax.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.NumberPicker
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.ads.AdInterface
import com.futurecode.hdcameramax.ads.interstitial_ad.FullScreenAdsHelper
import com.futurecode.hdcameramax.ads.reward.RewardAdsHelper
import com.futurecode.hdcameramax.databinding.DialogPremiumAdBinding
import com.futurecode.hdcameramax.model.Promo
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.gson.Gson
import org.json.JSONArray
import java.lang.reflect.Field
import java.util.Locale
import java.util.Random

object Utils {
    fun Float.dpToPx(activity: Activity): Float =
        this * activity.resources.displayMetrics.density

    fun NumberPicker.removeDividers() {
        try {
            val fields: Array<Field> = NumberPicker::class.java.declaredFields
            for (field in fields) {
                if (field.name == "mSelectionDivider") {
                    field.isAccessible = true
                    field.set(this, null)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    fun NumberPicker.applySelectedColor(hexColor: String) {
        try {
            val selectorWheelPaintField =
                NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
            selectorWheelPaintField.isAccessible = true
            (selectorWheelPaintField.get(this) as android.graphics.Paint).apply {
                color = Color.parseColor(hexColor)
                textSize = 52f * resources.displayMetrics.scaledDensity
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.setFormatter { value -> value.toString() }
    }

    fun getWidth(context: Context): Int {
        val displayMetrics = DisplayMetrics()
        val windowmanager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowmanager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun formatTimestampToHours(timestamp: Long): String {
        val diffMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60)
        return when {
            diffMinutes < 60 -> "just now"
            else -> {
                val hours = diffMinutes / 60
                if (hours == 1L) "1 hr" else "$hours hr"
            }
        }
    }

    fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) return true
        }
        return false
    }

    fun openCustomChrome(activity: Activity, url: String) {
        try {
            val customIntent = CustomTabsIntent.Builder()
            customIntent.setToolbarColor(ContextCompat.getColor(activity, R.color.black))
            val customTabsIntent = customIntent.build()
            customTabsIntent.intent.setPackage("com.android.chrome")
            customTabsIntent.launchUrl(activity, Uri.parse(url))
        } catch (e: Exception) {
            openBrowser(activity, url)
        }
    }

    fun openBrowser(activity: Activity, url: String) {
        val link = url.ifBlank { "https://www.google.com" }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            activity.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(activity, "No browser found", Toast.LENGTH_LONG).show()
        }
    }

    fun jsonToStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    fun getRandomUrls(context: Context): String {
        val list = jsonToStringList(PrefManager.get(context).customUrls)
        return if (list.isNotEmpty()) {
            list[Random().nextInt(list.size)]
        } else {
            "https://www.google.com/"
        }
    }



    fun View.setAdClickListener(
        activity: Activity,
        adsHelper: FullScreenAdsHelper,
        isShowEveryTime: Boolean = false,
        onFinished: () -> Unit
    ) {
        setOnClickListener {
            ProgressBarUtils.showProgressDialog(activity)
            adsHelper.showInterstitialAds(isShowEveryTime, object : AdInterface {
                override fun finished() {
                    ProgressBarUtils.hideProgressDialog()
                    onFinished()
                }
            })
        }
    }


    /**
     * Corrected method: Initializes PrefManager using the local context
     * to avoid a NullPointerException on app startup.
     */
    fun updateBaseContextLocale(context: Context): Context {
        val pref = PrefManager.get(context)
        val languageCode = pref.selectedLanguage
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        config.fontScale = 1.0f

        // Use deprecated updateConfiguration for broader resource coverage
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        return context.createConfigurationContext(config)
    }

    fun getPromosListFromPrefs(): List<Promo> {
        return try {
            val jsonString = MyApplication.app.prefManager.promosList

            if (jsonString.isNullOrEmpty()) return emptyList()

            Gson().fromJson(
                jsonString,
                Array<Promo>::class.java
            )?.toList() ?: emptyList()

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }



    // ====================================================================
// 👑 1. FIXED EXTENSION FUNCTION: Correctly mapping the interface anonymous layers
// ====================================================================
    fun Fragment.showRewardAdDialog(
        onRewardEarned: () -> Unit,
        onRewardNotEarned: () -> Unit = {}
    ) {
        if (!isAdded || view == null) {
            onRewardNotEarned()
            return
        }

        val dialog = BaseDialog(requireActivity(), R.style.TransparentDialog)
        val binding = DialogPremiumAdBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setCancelable(false)
        dialog.bind(binding) {
            btnYes.setOnClickListener {
                dialog.dismiss()

                // Variable to track tracking token inside the full lifecycle pipeline scope securely
                var isRewardEarnedInSession = false

                // ✅ FIXED SYNC: Instantiating the explicit RewardAdInterface signature expected by helper
                RewardAdsHelper(requireActivity()).showRewardAds(object : RewardAdsHelper.RewardAdInterface {
                    override fun onAdShown() {
                        Log.d("RewardDialog", "Ad presentation verified successfully.")
                    }

                    override fun onUserEarnedReward(rewardItem: RewardItem) {
                        // Flips verification lock safely to true when user fully finishes watch conditions
                        isRewardEarnedInSession = true
                    }

                    override fun onAdClosed() {
                        // Triggers the respective final callback only after ad window screen dismisses completely
                        if (isRewardEarnedInSession && view != null) {
                            onRewardEarned()
                        } else {
                            onRewardNotEarned()
                        }
                    }

                    override fun onAdFailed(error: String) {
                        Log.e("RewardDialog", "Ad execution processing dropped: $error")
                        onRewardNotEarned()
                    }
                })
            }

            btnNo.setOnClickListener {
                dialog.dismiss()
                onRewardNotEarned()
            }
        }
        dialog.show()
    }
}