package com.example.myproject

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.Fragment.loginandregister.SignInFragment
import com.example.myproject.Fragment.admin.AdminProgramFragment
import com.example.myproject.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            // ยังไม่ล็อกอิน
            replaceFragment(SignInFragment.newInstance())
        } else {
            // ถ้าล็อกอินแล้ว ตรวจสอบ role ก่อนเข้า
            checkUserRole()
        }
    }

    private fun checkUserRole() {
        val user = auth.currentUser ?: return
        val sharedPref = getSharedPreferences("running_app_prefs", Context.MODE_PRIVATE)
        val cachedRole = sharedPref.getString("user_role", null)

        if (cachedRole != null) {
            // ถ้ามี role ใน local แล้ว
            openCorrectFragment(cachedRole)
        } else {
            // ถ้ายังไม่มี ให้โหลดจาก Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role") ?: "user"
                    sharedPref.edit().putString("user_role", role).apply()
                    openCorrectFragment(role)
                }
                .addOnFailureListener {
                    openCorrectFragment("user")
                }
        }
    }

    private fun openCorrectFragment(role: String) {
        if (role == "admin") {
            replaceFragment(AdminProgramFragment())
        } else {
            replaceFragment(MainFragment.newInstance())
        }
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.containerMain.id, fragment)
            .commit()
    }
}
