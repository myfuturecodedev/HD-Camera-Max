package com.futurecode.hdcameramax.notification

import com.futurecode.hdcameramax.utils.getNotificationListFromPrefs


object NotificationRepository {
    val notifications = getNotificationListFromPrefs()

}