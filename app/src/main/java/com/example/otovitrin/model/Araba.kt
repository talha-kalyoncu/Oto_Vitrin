package com.example.otovitrin.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Araba(
    val ilanId: String? = null,
    val baslik: String? = null,
    val fiyat: Int? = null,
    val konum: String? = null,
    val marka: String? = null,
    val model: String? = null,
    val yil: Int? = null,
    val km: Int? = null,
    val yakit: String? = null,
    val vites: String? = null,
    val tramer: Int? = null,
    val aciklama: String? = null,
    val gorselUrl: String? = null,
    val kullaniciId: String? = null,
    val ilanTarihi: Timestamp? = null
) : Parcelable