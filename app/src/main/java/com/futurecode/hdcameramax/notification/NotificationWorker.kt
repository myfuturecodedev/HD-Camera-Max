package com.futurecode.hdcameramax.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.futurecode.hdcameramax.activity.MyApplication
import java.util.concurrent.TimeUnit



class NotificationWorker(appContext: Context, params: WorkerParameters) :
    Worker(appContext, params) {

    override fun doWork(): Result {
        return try {
            val list = NotificationRepository.notifications

            if (list.isEmpty()) {
                MyApplication.app.prefManager.isNotificationStarts = false
                return Result.failure()
            }

            val prefs = MyApplication.app.prefManager
            val index = prefs.getNextNotificationIndex(list.size)
            val item = list[index]

            showNotification(item.banner, item.title, item.description)
            scheduleNextWorker()

            MyApplication.app.prefManager.isNotificationStarts = true
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            MyApplication.app.prefManager.isNotificationStarts = false
            Result.failure()
        }
    }

    private fun showNotification(icon: String, title: String, message: String) {
        val channelId = "recover_deleted_message_local_channel"
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recover Deleted Message Local Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        var url = MyApplication.app.prefManager.notificationUrl
        if (url.isEmpty()) url = "https://706.mark.qureka.com/intro/question"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            Glide.with(applicationContext)
                .asBitmap()
                .load(icon)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val notification = NotificationCompat.Builder(applicationContext, channelId)
                            .setSmallIcon(com.futurecode.hdcameramax.R.mipmap.ic_launcher_round)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setStyle(
                                NotificationCompat.BigPictureStyle()
                                    .bigPicture(resource)
                                    .bigLargeIcon(null as Bitmap?)
                            )
                            .setLargeIcon(resource)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()

                        manager.notify(System.currentTimeMillis().toInt(), notification)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        showSimpleNotification(channelId, title, message, pendingIntent, manager)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            showSimpleNotification(channelId, title, message, pendingIntent, manager)
        }
    }

    private fun showSimpleNotification(
        channelId: String,
        title: String,
        message: String,
        pendingIntent: PendingIntent,
        manager: NotificationManager
    ) {
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(com.futurecode.hdcameramax.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
    private fun scheduleNextWorker() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(5, TimeUnit.MINUTES)
            .addTag("recover_deleted_message_notification_worker")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "recover_deleted_message_notification_chain",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
}