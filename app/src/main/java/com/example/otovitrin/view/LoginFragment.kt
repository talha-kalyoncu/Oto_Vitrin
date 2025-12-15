package com.example.otovitrin.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.otovitrin.R
import com.example.otovitrin.databinding.FragmentLogin2Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private var _binding: FragmentLogin2Binding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogin2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        val guncelKullanici = auth.currentUser
        if (guncelKullanici != null) {
            findNavController().navigate(R.id.action_loginFragment_to_anaSayfaFragment)
        }

        binding.girisbutton.setOnClickListener {
            girisIsleminiYap()
        }

        binding.KayitOltxtButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_kayitFragment)
        }
        binding.SifemiUnuttumtxtButton.setOnClickListener {
            val dialog = SifreUnuttumDialog()
            dialog.show(parentFragmentManager, "SifreUnuttumDialog")
        }
    }

    private fun girisIsleminiYap() {
        val email = binding.EmailEditText.text.toString().trim()
        val sifre = binding.PasswordEditText.text.toString().trim()

        if (email.isEmpty() || sifre.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen email ve şifrenizi girin!", Toast.LENGTH_LONG).show()
        }else {


            auth.signInWithEmailAndPassword(email, sifre)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user!!.uid


                    firestore.collection("Users").document(userId).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {

                                val hesapAktif = document.getBoolean("hesapAktif") ?: true

                                if (hesapAktif) {
                                    val adSoyad = document.getString("adSoyad")
                                    val rol = document.getString("rol")
                                    Toast.makeText(requireContext(), "Hoşgeldin $adSoyad ", Toast.LENGTH_LONG).show()
                                    findNavController().navigate(R.id.action_loginFragment_to_anaSayfaFragment)
                                } else {
                                    //hesap donuksa alert dialog a gider
                                    alertHesapAc(userId)
                                }

                            } else {
                                Toast.makeText(requireContext(), "Kullanıcı verisi bulunamadı!", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Giriş Başarısız: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Giriş Başarısız: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Donuk hesabı açmak için alert dialog bölümü
    private fun alertHesapAc(userId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Hesabınız Dondurulmuş")
        builder.setMessage("Hesabınızı daha önce dondurmuşsunuz. Tekrar aktif edip giriş yapmak ister misiniz?")
        builder.setCancelable(false)

        builder.setPositiveButton("EVET, AÇ") { _, _ ->

            firestore.collection("Users").document(userId).update("hesapAktif", true)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Hesabınız Aktif Edildi! Hoşgeldiniz.", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_loginFragment_to_anaSayfaFragment)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Hata oluştu: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        builder.setNegativeButton("HAYIR") { _, _ ->
            auth.signOut()
            Toast.makeText(requireContext(), "Giriş iptal edildi.", Toast.LENGTH_LONG).show()
        }

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}