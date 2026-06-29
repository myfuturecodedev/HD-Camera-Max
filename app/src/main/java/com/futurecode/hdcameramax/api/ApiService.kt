package com.futurecode.hdcameramax.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {

    @GET("sagar_apps/get_json.php")
    suspend fun getJson(
        @Query("file") fileName: String,
        @Header("X-APP-KEY") appKey: String
    ): Response<ResponseBody>


}