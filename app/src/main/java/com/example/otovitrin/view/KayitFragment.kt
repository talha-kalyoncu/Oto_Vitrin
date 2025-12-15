package com.example.otovitrin.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.otovitrin.view.KayitFragmentDirections
import com.example.otovitrin.R
import com.example.otovitrin.databinding.FragmentKayitBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class KayitFragment : Fragment() {

    private var _binding: FragmentKayitBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKayitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.kayitbutton.setOnClickListener {
            kayitIsleminiBaslat()
        }

        binding.GirisYaptxtButton.setOnClickListener {
            findNavController().navigate(R.id.action_kayitFragment_to_loginFragment)
        }
    }

    private fun kayitIsleminiBaslat() {
        val adSoyad = binding.editTextText3.text.toString().trim()
        val email = binding.EmailKayitEditText.text.toString().trim()
        val sifre = binding.PasswordKayitEditText.text.toString().trim()
        val hamTelefon = binding.editTextPhone.text.toString().trim()
        val tamTelefon = "+90$hamTelefon"

        if (email.isNotEmpty() && sifre.isNotEmpty() && adSoyad.isNotEmpty() && hamTelefon.isNotEmpty() && hamTelefon.length == 10) {
            if (sifre.length < 6) {
                Toast.makeText(requireContext(), "Şifreniz en az 6 karakterden oluşmalı", Toast.LENGTH_LONG).show()
            } else {
                auth.createUserWithEmailAndPassword(email, sifre).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = task.result.user!!.uid

                        val kullaniciVerisi = hashMapOf(
                            "kullaniciId" to userId,
                            "adSoyad" to adSoyad,
                            "email" to email,
                            "telefon" to tamTelefon,
                            "rol" to "User",
                            "hesapAktif" to true
                        )

                        firestore.collection("Users").document(userId).set(kullaniciVerisi)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Kayıt Başarılı!", Toast.LENGTH_SHORT).show()
                                val action = KayitFragmentDirections.Companion.actionKayitFragmentToAnaSayfaFragment()
                                findNavController().navigate(action)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Veritabanı Hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun ve telefon numaranızdan emin olun!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}