package com.futurecode.hdcameramax.ui.afterlogin

import android.content.Context
import android.net.Uri

class FavouriteRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFavourite(uri: Uri): Boolean {
        return favouriteUris().contains(uri.toString())
    }

    fun toggleFavourite(uri: Uri): Boolean {
        val uriString = uri.toString()
        val updated = favouriteUris().toMutableSet()
        val isNowFavourite = if (updated.contains(uriString)) {
            updated.remove(uriString)
            false
        } else {
            updated.add(uriString)
            true
        }
        prefs.edit().putStringSet(KEY_FAVOURITES, updated).apply()
        return isNowFavourite
    }

    fun removeFavourite(uri: Uri) {
        val updated = favouriteUris().toMutableSet()
        updated.remove(uri.toString())
        prefs.edit().putStringSet(KEY_FAVOURITES, updated).apply()
    }

    fun favouriteUris(): Set<String> {
        return prefs.getStringSet(KEY_FAVOURITES, emptySet()).orEmpty()
    }

    private companion object {
        const val PREFS_NAME = "hd_camera_favourites"
        const val KEY_FAVOURITES = "favourite_media_uris"
    }
}
