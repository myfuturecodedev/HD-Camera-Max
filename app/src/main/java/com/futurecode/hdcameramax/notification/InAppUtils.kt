package com.futurecode.hdcameramax.notification

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.view.View
import com.bumptech.glide.Glide
import com.futurecode.hdcameramax.R
import com.futurecode.hdcameramax.activity.MyApplication
import com.futurecode.hdcameramax.databinding.PromoDialogBinding
import com.futurecode.hdcameramax.utils.Utils
import com.futurecode.hdcameramax.utils.Utils.getPromosListFromPrefs
import java.util.Objects


data class PromoData(
    val title: String,
    val description: String,
    val button: String,
    val image: Int
)

object InAppUtils {
    private var bannerTimer: CountDownTimer? = null // keep reference to current timer

    fun showInAppBanner(activity: Activity, isShowEveryTime: Boolean) {
        val myPreferenceHelper = MyApplication.app.prefManager
        val url = myPreferenceHelper.clickUrl/*.getString(AppConstants.CLICK_URL)*/
        if (url.isEmpty()) {
            return
        }
        val clickLimit = myPreferenceHelper.clickLimit
        val count: Int = myPreferenceHelper.clickCount
        if (isShowEveryTime) {
            showBanner(activity)
        } else {
            if (count % clickLimit == 0) {
                showBanner(activity)
            }
        }
        myPreferenceHelper.clickCount = count + 1
    }

    private fun showBanner(activity: Activity) {
        // Cancel old timer if exists
        InAppUtils.bannerTimer?.cancel()

        val builder = AlertDialog.Builder(activity)
        val view: View = activity.layoutInflater.inflate(R.layout.promo_dialog, null)
        builder.setView(view)
        builder.setCancelable(false)
        val bind: PromoDialogBinding = PromoDialogBinding.bind(view)
        val dialog = builder.create()
        Objects.requireNonNull(dialog.window)
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val list = getPromosListFromPrefs()
        if (list.isNotEmpty()) {
            val data = list[InAppUtils.getNextPromoIndex(
                list.size
            )]
            data.let {
                bind.tvTitle.text = it.title
                bind.tvTitle2.text = it.description
                bind.btnUpgrade.text = it.buttonText
                Glide.with(activity).load(it.image)
                    .into(bind.imgBanner)
                // Start countdown timer for 2 minutes (120000 ms)
                val totalTime = 2 * 60 * 1000L // 2 minutes
                InAppUtils.bannerTimer = object : CountDownTimer(totalTime, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val minutes = (millisUntilFinished / 1000) / 60
                        val seconds = (millisUntilFinished / 1000) % 60
                        bind.tvTimer.text = "(${String.format("%02d:%02d", minutes, seconds)})"
                    }

                    override fun onFinish() {
                        bind.tvTimer.text = ""
                    }
                }.start()

                bind.btnUpgrade.setOnClickListener { v ->
                    Utils.openCustomChrome(activity, it.link ?: "")
                    InAppUtils.bannerTimer?.cancel()
                    dialog.dismiss()
                }
                bind.btnMaybeLater.setOnClickListener { v ->
                    Utils.openCustomChrome(activity, it.link ?: "")
                    InAppUtils.bannerTimer?.cancel()
                    dialog.dismiss()
                }
                bind.btnClose.setOnClickListener { v ->
                    Utils.openCustomChrome(activity, it.link ?: "")
                    InAppUtils.bannerTimer?.cancel()
                    dialog.dismiss()
                }
                bind.root.setOnClickListener { v ->
                    Utils.openCustomChrome(activity, it.link ?: "")
                    InAppUtils.bannerTimer?.cancel()
                    dialog.dismiss()
                }

                dialog.show()
            }
        }

    }

    fun getNextPromoIndex(total: Int): Int {
        val prefs = MyApplication.app.prefManager
        val currentIndex = prefs.promoIndex
        val nextIndex = (currentIndex + 1) % total
        prefs.promoIndex = nextIndex
        return currentIndex
    }

    fun getPromoList(index: Int): PromoData {
        val promoList = listOf(
            PromoData(
                title = "🚀 Upgrade Diwali Bundle",
                description = "BLUE FLAME DRA",
                button = "Upgrade Now",
                image = R.drawable.inapp1
            ),

            PromoData(
                title = "🚀 Upgrade Diwali Bundle",
                description = "WALL - NINJA'S ARSE",
                button = "Upgrade Now",
                image = R.drawable.inapp1
            ),

            PromoData(
                title = "🚀 Upgrade Diwali Bundle",
                description = "ETERNAL ESSENCE BUNDLE",
                button = "Upgrade Now",
                image = R.drawable.inapp1
            ),

            PromoData(
                title = "🚀 Upgrade Diwali Bundle",
                description = "Avenger Bundle",
                button = "Upgrade Now",
                image = R.drawable.inapp1
            ),
            PromoData(
                title = "🚀 Upgrade Diwali Bundle",
                description = "Trouble Bundle",
                button = "Upgrade Now",
                image = R.drawable.inapp1
            ),
            PromoData(
                title = "🚀 Upgrade Diwali Bundle",
                description = "Malachite Ninja Bun",
                button = "Upgrade Now",
                image = R.drawable.inapp1
            )
        )
        return promoList[index]
    }
}