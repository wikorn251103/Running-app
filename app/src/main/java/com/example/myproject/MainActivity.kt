package com.example.myproject

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.*
import com.example.Fragment.loginandregister.SignInFragment
import com.example.myproject.Fragment.workout.WorkoutScheduler
import com.example.myproject.Fragment.admins.AdminDashboardFragment
import com.example.myproject.Fragment.training.MissedWorkoutCheckWorker
import com.example.myproject.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

        // 🕒 เริ่มระบบเช็คการซ้อมอัตโนมัติของ WorkoutScheduler
        WorkoutScheduler.scheduleDailyCheck(this)

        // 🕒 ตั้ง WorkManager ให้ตรวจ missed workout ทุกวันตอน 00:01 น.
        scheduleDailyMissedWorkoutCheck()

        // 🔑 ตรวจสอบสถานะผู้ใช้
        if (auth.currentUser == null) {
            replaceFragment(SignInFragment.newInstance(), addToBackStack = false)
        } else {
            checkUserRole()
        }
    }

    /**
     * ตรวจสอบ role ของผู้ใช้จาก Firestore
     */
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

    /**
     * เปิด fragment ตาม role
     */
    private fun openCorrectFragment(role: String) {
        if (role == "admin") {
            replaceFragment(AdminDashboardFragment(), addToBackStack = false, MainFragment.TAG)
        } else {
            replaceFragment(MainFragment.newInstance(), addToBackStack = false, MainFragment.TAG)
        }
    }

    /**
     * เปลี่ยน Fragment แบบปกติ
     */
    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true, tag: String? = null) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(binding.containerMain.id, fragment, tag)

        if (addToBackStack) transaction.addToBackStack(null)
        transaction.commit()
    }

    /**
     * เปลี่ยน Fragment พร้อมล้าง back stack
     */
    fun replaceFragmentClearBackStack(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(binding.containerMain.id, fragment)
            .commit()
    }

    /**
     * ล้าง role ผู้ใช้ใน SharedPreferences
     */
    fun clearUserRole() {
        sharedPref.edit().remove("user_role").apply()
    }

    /**
     * 🕐 ตั้งค่า WorkManager ให้ทำงานทุกวันเวลา 00:01 น.
     */
    private fun scheduleDailyMissedWorkoutCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val delayUntilMidnight = calculateDelayUntilMidnight()

        val workRequest = PeriodicWorkRequestBuilder<MissedWorkoutCheckWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(delayUntilMidnight, TimeUnit.MILLISECONDS)
            .addTag("MissedWorkoutCheck")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyMissedWorkoutCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        Log.d("MainActivity", "✅ WorkManager scheduled for daily missed workout check")
    }

    /**
     * 🕐 คำนวณเวลาจนถึงเที่ยงคืนถัดไป (00:01 น.)
     */
    private fun calculateDelayUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = midnight.timeInMillis - now.timeInMillis
        Log.d("MainActivity", "⏰ Next check will be in ${delay / 1000 / 60 / 60} hours")

        return delay
    }
}
