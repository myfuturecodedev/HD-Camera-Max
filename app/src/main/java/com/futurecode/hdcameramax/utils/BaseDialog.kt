package com.futurecode.hdcameramax.utils

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.viewbinding.ViewBinding
import com.futurecode.hdcameramax.R

open class BaseDialog(
    protected val activity: Activity,
    style: Int = R.style.TransparentDialog
) : Dialog(activity, style) {

    // ── Config ─────────────────────────────────────────────────────────────────
    /** Width as percent of screen width (0..100). Default 90. */
    var widthPercent: Int = 90

    /** Gravity of the dialog on screen. Default CENTER. */
    var dialogGravity: Int = Gravity.CENTER

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyWindowStyle()
    }

    override fun show() {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            super.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dismiss() {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            super.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Convenience: bind layout without subclassing ───────────────────────────

    /**
     * Inflate, attach, and configure binding in one call.
     *
     * val dialog = BaseDialog(requireActivity())
     * dialog.bind(DialogSaveResultBinding.inflate(layoutInflater)) {
     *     tvTitle.text = "Save Result"
     *     btnConfirm.setAdClickListener(requireActivity(), fullScreenAdsHelper) { dialog.dismiss() }
     * }
     * dialog.show()
     */
    fun <VB : ViewBinding> bind(binding: VB, block: VB.() -> Unit): BaseDialog {
        setContentView(binding.root)
        binding.block()
        return this
    }

    // ── Window styling ─────────────────────────────────────────────────────────

    private fun applyWindowStyle() {
        window?.apply {
            // Remove default white background so CardView corners show through
            setBackgroundDrawableResource(android.R.color.transparent)

            // Width
            val screenWidth = activity.resources.displayMetrics.widthPixels
            setLayout(
                screenWidth * widthPercent / 100,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Gravity
            setGravity(dialogGravity)

            // Dim
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes?.apply { dimAmount = 0.5f }

            // Keyboard resize
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
}