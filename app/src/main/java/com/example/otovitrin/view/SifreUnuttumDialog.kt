package com.example.otovitrin.view

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.otovitrin.databinding.DialogSifreUnuttumBinding
import com.google.firebase.auth.FirebaseAuth

class SifreUnuttumDialog : DialogFragment() {

    private var _binding: DialogSifreUnuttumBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSifreUnuttumBinding.inflate(inflater, container, false)
        return binding.root
    }

    //pencere boyutu ayarlama
    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.btnGonder.setOnClickListener {
            val email = binding.editSifreMail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen mail adresinizi yazın.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // firebase şifre sıfırlama
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Sıfırlama maili gönderildi!", Toast.LENGTH_LONG).show()
                    dismiss() // başarılıysa pencereyi kapat
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        //iptal
        binding.btnIptal.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}