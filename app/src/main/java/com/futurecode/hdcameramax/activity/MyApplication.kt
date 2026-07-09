package com.futurecode.hdcameramax.activity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.futurecode.hdcameramax.utils.JsonReadUtils
import com.futurecode.hdcameramax.utils.PrefManager
import java.util.Locale
import kotlin.collections.get

class MyApplication: Application() {
    lateinit var prefManager: PrefManager

    private var currentActivity: Activity? = null


    override fun attachBaseContext(base: Context) {
        // Safe locale initialization for Application context
        val lang = PrefManager.get(base).selectedLanguage
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        // Using createConfigurationContext is the safe way for attachBaseContext
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }



    override fun onCreate() {
        super.onCreate()
        app = this
        prefManager = PrefManager.get(this)

       // JsonReadUtils.fetchJsonData(this)


    }

    companion object {
        lateinit var app: MyApplication

        fun setLocale(context: Context): Context {
            val prefManager = PrefManager.get(context)
            val languageCode = prefManager.selectedLanguage ?: "en"
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)

            // This is the key: Update the context immediately
            context.resources.updateConfiguration(config, context.resources.displayMetrics)

            // Return a localized context for attachBaseContext
            return context.createConfigurationContext(config)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val languageCode = app.prefManager.selectedLanguage ?: "en"
        val locale = Locale(languageCode)
        newConfig.setLocale(locale)
        Locale.setDefault(locale)
    }
}