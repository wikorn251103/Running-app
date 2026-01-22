package com.example.myproject

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.work.*
import com.example.Fragment.loginandregister.SignInFragment
import com.example.myproject.Fragment.workout.WorkoutScheduler
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

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ ตั้งค่า Status Bar ให้เป็นสีน้ำเงิน
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, R.color.darkBlue)

        // ตั้งค่าไอคอนใน status bar ให้เป็นสีขาว (เพราะพื้นหลังเป็นสีน้ำเงินเข้ม)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // ใช้ไอคอนสีขาว
        }

        //  แก้ปัญหา UI ชน StatusBar ตรงนี้
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                0,  // ไม่ใส่ padding top เพราะต้องการให้ UI แสดงใต้ status bar
                systemBars.right,
                0   // เปลี่ยนจาก systemBars.bottom เป็น 0 เพื่อไม่ให้มี padding ด้านล่าง
            )
            insets
        }

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
     * ✅ แสดงปุ่มเมนูด้านล่าง (เรียกผ่าน MainFragment) - ป้องกัน crash
     */
    fun showBottomNavigation() {
        try {
            val mainFragment = supportFragmentManager.findFragmentByTag(MainFragment.TAG) as? MainFragment
            if (mainFragment != null && mainFragment.isAdded) {
                mainFragment.setBottomNavVisible(true)
                Log.d(TAG, "✅ Bottom Navigation shown")
            } else {
                Log.w(TAG, "⚠️ MainFragment not found or not added yet")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing bottom navigation: ${e.message}", e)
        }
    }

    /**
     * ✅ ซ่อนปุ่มเมนูด้านล่าง (เรียกผ่าน MainFragment) - ป้องกัน crash
     */
    fun hideBottomNavigation() {
        try {
            val mainFragment = supportFragmentManager.findFragmentByTag(MainFragment.TAG) as? MainFragment
            if (mainFragment != null && mainFragment.isAdded) {
                mainFragment.setBottomNavVisible(false)
                Log.d(TAG, "❌ Bottom Navigation hidden")
            } else {
                Log.w(TAG, "⚠️ MainFragment not found or not added yet")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error hiding bottom navigation: ${e.message}", e)
        }
    }

    /**
     * ✅ แสดง/ซ่อนปุ่มเมนูตารางซ้อม (เรียกจาก HomeFragment)
     */
    fun updateScheduleMenuVisibility(isVisible: Boolean) {
        try {
            val mainFragment = supportFragmentManager.findFragmentByTag(MainFragment.TAG) as? MainFragment
            if (mainFragment != null && mainFragment.isAdded) {
                mainFragment.updateScheduleMenuVisibility(isVisible)
                Log.d(TAG, "📱 Schedule menu visibility updated: $isVisible")
            } else {
                Log.w(TAG, "⚠️ MainFragment not found or not added yet")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating schedule menu visibility: ${e.message}", e)
        }
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

        Log.d(TAG, "✅ WorkManager scheduled for daily missed workout check")
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
        Log.d(TAG, "⏰ Next check will be in ${delay / 1000 / 60 / 60} hours")

        return delay
    }
}