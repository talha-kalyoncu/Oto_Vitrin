package com.example.otovitrin.model
import com.google.gson.annotations.SerializedName

data class BenzinResponse(
    @SerializedName("result") val result: List<BenzinItem>?,
    @SerializedName("success") val success: Boolean?
)

data class BenzinItem(
    @SerializedName("katilim") val marka: String?, //benzin dizel
    @SerializedName("benzin") val fiyat: Double?
)