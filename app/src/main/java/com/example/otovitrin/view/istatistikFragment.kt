package com.example.otovitrin.view

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment // 1. DEĞİŞİKLİK: Burası Fragment değil DialogFragment olmalı
import com.example.otovitrin.databinding.FragmentIstatistikBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class istatistikFragment : DialogFragment() {

    private var _binding: FragmentIstatistikBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIstatistikBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        verileriHesapla()

        binding.btnKapat.setOnClickListener {
            dismiss()
        }
    }

    private fun verileriHesapla() {
        //yüklenirken ... gözüksün
        binding.txtStatIlanSayisi.text = "..."
        binding.txtStatOrtFiyat.text = "..."
        binding.txtStatUyeSayisi.text = "..."
        binding.txtStatPasifUye.text = "..."

        firestore.collection("Ilanlar").get().addOnSuccessListener { ilanDocuments ->
            val toplamIlan = ilanDocuments.size()
            var toplamFiyat: Long = 0

            for (doc in ilanDocuments) {
                val fiyat = doc.getLong("fiyat") ?: 0
                toplamFiyat += fiyat
            }

            val ortalama = if (toplamIlan > 0) toplamFiyat / toplamIlan else 0
            binding.txtStatIlanSayisi.text = toplamIlan.toString()
            binding.txtStatOrtFiyat.text = "${formatla(ortalama)} ₺"

            firestore.collection("Users").get().addOnSuccessListener { userDocuments ->
                val toplamUye = userDocuments.size()
                var pasifUyeSayaci = 0

                for (user in userDocuments) {
                    val hesapAktifMi = user.getBoolean("hesapAktif")
                    if (hesapAktifMi == false) {
                        pasifUyeSayaci++
                    }
                }
                binding.txtStatUyeSayisi.text = toplamUye.toString()
                binding.txtStatPasifUye.text = pasifUyeSayaci.toString()
            }
        }
    }

    private fun formatla(fiyat: Long): String {
        val semboller = DecimalFormatSymbols()
        semboller.groupingSeparator = '.'
        val formatter = DecimalFormat("#,###", semboller)
        return formatter.format(fiyat)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}