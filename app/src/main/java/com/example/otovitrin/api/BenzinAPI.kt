package com.example.otovitrin.api

import com.example.otovitrin.model.BenzinResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface BenzinAPI {

    @Headers(
        "content-type: application/json",
        "authorization: apikey 3t0Ied7BJ8kJ4RdGisvmej:1cG7PZ86LwKtKu80Vqgucg"
                      //Collect API kullanıyoruz
    )
    @GET("gasPrice/turkeyGasoline")
    fun benzinFiyatlariniGetir(
        @Query("city") sehir: String = "istanbul"
    ): Call<BenzinResponse>
}