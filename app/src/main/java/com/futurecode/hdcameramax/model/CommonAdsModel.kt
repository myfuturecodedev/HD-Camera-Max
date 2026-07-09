package com.futurecode.hdcameramax.model


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CommonAdsModel(
    @SerializedName("data")
    val data: Data?,
    @SerializedName("meta")
    val meta: Meta?
)

@Keep
data class Ads(
    @SerializedName("admob_app_open")
    val admobAppOpen: String?,
    @SerializedName("admob_banner")
    val admobBanner: String?,
    @SerializedName("admob_interstitial")
    val admobInterstitial: String?,
    @SerializedName("admob_native")
    val admobNative: String?,
    @SerializedName("admob_native_banner")
    val admobNativeBanner: String?,
    @SerializedName("admob_reward")
    val admobReward: String?,
    @SerializedName("ads_frequency")
    val adsFrequency: Int?,
    @SerializedName("ads_status")
    val adsStatus: String?,
    @SerializedName("custom_ads_status")
    val customAdsStatus: String?,
    @SerializedName("custom_url")
    val customUrl: List<String?>?,
    @SerializedName("inter_ads_flow")
    val interAdsFlow: List<String?>?,
    @SerializedName("meta_interstitial")
    val metaInterstitial: String?,
    @SerializedName("meta_native")
    val metaNative: String?,
    @SerializedName("meta_native_banner")
    val metaNativeBanner: String?,
    @SerializedName("native_ads_flow")
    val nativeAdsFlow: List<String?>?,
    @SerializedName("native_banner_ads_flow")
    val nativeBannerAdsFlow: List<String?>?
)

@Keep
data class AppSettings(
    @SerializedName("contact_us")
    val contactUs: String?,
    @SerializedName("inappbanner_click_limit")
    val inappbannerClickLimit: Int?,
    @SerializedName("notification_url")
    val notificationUrl: String?,
    @SerializedName("notifications")
    val notifications: List<Notification?>?,
    @SerializedName("plan1_text")
    val plan1Text: String?,
    @SerializedName("plan2_text")
    val plan2Text: String?,
    @SerializedName("plan3_text")
    val plan3Text: String?,
    @SerializedName("privacy_policy")
    val privacyPolicy: String?,
    @SerializedName("promos")
    val promos: List<Promo?>?,
    @SerializedName("subscription_page")
    val subscriptionPage: String?,
    @SerializedName("inappbanner_url")
    val inappbannerUrl: String?
)

@Keep
data class Data(
    @SerializedName("ads")
    val ads: Ads?,
    @SerializedName("app_settings")
    val appSettings: AppSettings?
)

@Keep
data class Meta(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("status")
    val status: String?
)

@Keep
data class Notification(
    @SerializedName("banner")
    val banner: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("link")
    val link: String?,
    @SerializedName("title")
    val title: String?
)


@Keep
data class Promo(
    @SerializedName("buttonText")
    val buttonText: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("image")
    val image: String?,
    @SerializedName("link")
    val link: String?,
    @SerializedName("title")
    val title: String?
)