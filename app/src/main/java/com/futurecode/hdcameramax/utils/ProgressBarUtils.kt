package com.futurecode.hdcameramax.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.futurecode.hdcameramax.R

class ProgressBarUtils {
    val isLoading: Boolean
        get() = dialogBuilder != null && dialogBuilder!!.isShowing

    companion object {
        var dialogBuilder: Dialog? = null
        fun showProgressDialog(context: Context) {
            if (dialogBuilder != null) dialogBuilder!!.dismiss()

            dialogBuilder = Dialog(context, R.style.MaterialSearch)
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val dialogView: View = inflater.inflate(R.layout.progress_dialog, null)
            dialogBuilder!!.setContentView(dialogView)
            dialogBuilder!!.setCancelable(false)
            dialogBuilder!!.setCanceledOnTouchOutside(false)
            try {
                dialogBuilder!!.show()
            } catch (e: WindowManager.BadTokenException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun hideProgressDialog() {
            try {
                if (dialogBuilder != null) dialogBuilder!!.dismiss()
            } catch (e: WindowManager.BadTokenException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}