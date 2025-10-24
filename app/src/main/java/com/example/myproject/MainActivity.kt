package com.example.myproject

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.Fragment.loginandregister.SignInFragment
import com.example.myproject.Fragment.workout.WorkoutScheduler
import com.example.myproject.Fragment.admins.AdminDashboardFragment
import com.example.myproject.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private val sharedPref by lazy { getSharedPreferences("running_app_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // เริ่มระบบเช็คการซ้อมอัตโนมัติ
        WorkoutScheduler.scheduleDailyCheck(this)

        if (auth.currentUser == null) {
            replaceFragment(SignInFragment.newInstance(), addToBackStack = false)
        } else {
            checkUserRole()
        }
    }

    private fun checkUserRole() {
        val user = auth.currentUser ?: return

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

    private fun openCorrectFragment(role: String) {
        if (role == "admin") {
            replaceFragment(AdminDashboardFragment(), addToBackStack = false)
        } else {
            replaceFragment(MainFragment.newInstance(), addToBackStack = false)
        }
    }

    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(binding.containerMain.id, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    fun replaceFragmentClearBackStack(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        supportFragmentManager.beginTransaction()
            .replace(binding.containerMain.id, fragment)
            .commit()
    }

    fun clearUserRole() {
        sharedPref.edit().remove("user_role").apply()
    }
}