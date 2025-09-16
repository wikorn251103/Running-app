package com.example.myproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.Fragment.loginandregister.SignInFragment
import com.example.myproject.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            // ยังไม่ล็อกอิน ให้เปิด LoginFragment ก่อน
            replaceFragment(SignInFragment.newInstance())
        } else {
            // ล็อกอินแล้ว เปิด MainFragment ที่มี bottom navigation
            replaceFragment(MainFragment.newInstance())
        }
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.containerMain.id, fragment)
            .addToBackStack(null)
            .commit()

    }
}
