package com.example.otovitrin.api

import com.example.otovitrin.model.DovizResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DovizAPI {
    // Adresimiz: https://api.frankfurter.app/latest?from=USD&to=TRY
    // https://api.frankfurter.app/latest?from=EUR&to=TRY
    @GET("latest")
    fun kurGetir(
        @Query("from") from: String, // Neyi bozduracağız usd yada euro
        @Query("to") to: String      // Neye çevireceğiz try
    ): Call<DovizResponse>
}