package com.example.otovitrin.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate // Eklendi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.otovitrin.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //uygulamaya girerken cihaz kullanıcısının tercih ettiği tema modu uygulanmasını sağlayan kod
        val sharedPref = getSharedPreferences("UygulamaAyarlari", MODE_PRIVATE)
        val karanlikModAcikMi = sharedPref.getBoolean("karanlikMod", false)

        if (karanlikModAcikMi) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}