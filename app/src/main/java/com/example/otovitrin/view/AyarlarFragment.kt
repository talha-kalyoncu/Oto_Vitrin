package com.example.otovitrin.view

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.otovitrin.R
import com.example.otovitrin.databinding.FragmentAyarlarBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    private var _binding: FragmentAyarlarBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAyarlarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        kullaniciBilgileriniGetir()

        binding.btnBilgileriGuncelle.setOnClickListener {
            bilgileriGuncelle()
        }

        binding.btnSifreDegistir.setOnClickListener {
            sifreyiDegistir()
        }

        binding.btnHesapDondur.setOnClickListener {
            hesabiDondur()
        }

        binding.btnCikisYap.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Çıkış Yapıldı", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
        }


        ayarlariYukle()
        binding.switchKaranlikMod.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                temaTercihiniKaydet(true)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                temaTercihiniKaydet(false)
            }
        }
    }

    private fun kullaniciBilgileriniGetir() {
        val user = auth.currentUser
        if (user != null) {
            binding.txtAyarlarEmailBaslik.text = user.email

            firestore.collection("Users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {

                        val adSoyad = document.getString("adSoyad")
                        binding.editAyarlarAdSoyad.setText(adSoyad)

                        var telefon = document.getString("telefon") ?: ""
                        if (telefon.startsWith("+90")) {
                            telefon = telefon.replace("+90", "")
                        }
                        binding.editAyarlarTelefon.setText(telefon)

                        val rol = document.getString("rol")

                        if (rol == "Admin") {
                            binding.btnHesapDondur.visibility = View.GONE
                        } else {
                            binding.btnHesapDondur.visibility = View.VISIBLE
                        }
                    }
                }
        }
    }

    private fun bilgileriGuncelle() {
        val yeniAd = binding.editAyarlarAdSoyad.text.toString().trim()
        val yeniTelHam = binding.editAyarlarTelefon.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (yeniAd.isEmpty() || yeniTelHam.isEmpty()) {
            Toast.makeText(requireContext(), "Alanlar boş bırakılamaz!", Toast.LENGTH_LONG).show()
            return
        }

        val yeniTelTam = "+90$yeniTelHam"

        if (userId != null) {
            val guncelVeri = hashMapOf<String, Any>(
                "adSoyad" to yeniAd,
                "telefon" to yeniTelTam
            )

            firestore.collection("Users").document(userId).update(guncelVeri)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profil Güncellendi", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Hata: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun sifreyiDegistir() {
        val user = auth.currentUser
        val eskiSifre = binding.editEskiSifre.text.toString()
        val yeniSifre = binding.editYeniSifre.text.toString()

        if (eskiSifre.isEmpty() || yeniSifre.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen eski ve yeni şifreyi girin", Toast.LENGTH_LONG).show()
        }else{

            if (yeniSifre.length < 6) {
            Toast.makeText(requireContext(), "Yeni şifre en az 6 karakter olmalı", Toast.LENGTH_LONG).show()
             }
                else{

                 if (user != null && user.email != null) {
                    val credential = EmailAuthProvider.getCredential(user.email!!, eskiSifre)

                 user.reauthenticate(credential)
                      .addOnSuccessListener {
                          user.updatePassword(yeniSifre)
                               .addOnSuccessListener {
                                   Toast.makeText(requireContext(), "Şifre Başarıyla Değiştirildi", Toast.LENGTH_LONG).show()
                                   binding.editEskiSifre.text.clear()
                                   binding.editYeniSifre.text.clear()
                            }.addOnFailureListener {
                                e -> Toast.makeText(requireContext(), "Değiştirilemedi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                      }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Eski şifreniz yanlış!", Toast.LENGTH_LONG).show()
                      }
                 }
                }
        }
    }

    private fun hesabiDondur() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Hesabı Dondur")
        builder.setMessage("Hesabınızı dondurduğunuzda YAYINDAKİ TÜM İLANLARINIZ SİLİNECEKTİR. Tekrar giriş yaptığınızda hesabınız açılır ama ilanlarınız geri gelmez.\n\nEmin misiniz?")

        builder.setPositiveButton("EVET, DONDUR") { _, _ ->

            val userId = auth.currentUser?.uid
            if (userId != null) {

                firestore.collection("Ilanlar")
                    .whereEqualTo("kullaniciId", userId)
                    .get()
                    .addOnSuccessListener { documents ->

                        // Batch açıyoruz hepsini içine atıp tek seferde yapacağız
                        val batch = firestore.batch()

                        for (doc in documents) {
                            batch.delete(doc.reference)
                        }

                        val userRef = firestore.collection("Users").document(userId)
                        batch.update(userRef, "hesapAktif", false)

                        batch.commit()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Hesap donduruldu ve ilanlar temizlendi.", Toast.LENGTH_LONG).show()

                                auth.signOut()
                                try {
                                    findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
                                } catch (e: Exception) { }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Hata oluştu: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
        }
        builder.setNegativeButton("İPTAL", null)
        builder.show()
    }

    private fun temaTercihiniKaydet(isDark: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences("UygulamaAyarlari", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("karanlikMod", isDark)
            apply()
        }
    }

    private fun ayarlariYukle() {
        val sharedPref = requireActivity().getSharedPreferences("UygulamaAyarlari", Context.MODE_PRIVATE)
        val isDark = sharedPref.getBoolean("karanlikMod", false)
        binding.switchKaranlikMod.isChecked = isDark
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}