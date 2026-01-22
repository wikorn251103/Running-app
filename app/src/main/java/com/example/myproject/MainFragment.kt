package com.example.myproject

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.example.Fragment.profile.ProfileFragment
import com.example.myproject.Fragment.training.TrainingScheduleFragment
import com.example.myproject.Fragment.article.list.ArticlesFragment
import com.example.myproject.Fragment.home.HomeFragment
import com.example.myproject.corre.BaseFragment
import com.example.myproject.databinding.FragmentMainBinding

class MainFragment : BaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate) {

    companion object {
        const val TAG = "MainFragment"
        fun newInstance() = MainFragment()
    }

    override fun initViews() {
        replaceFragment(HomeFragment.newInstance())

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_home -> {
                    replaceFragment(HomeFragment.newInstance())
                    true
                }
                R.id.bottom_calendar -> {
                    replaceFragment(TrainingScheduleFragment())
                    true
                }
                R.id.bottom_article -> {
                    replaceFragment(ArticlesFragment())
                    true
                }
                R.id.bottom_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        // ✅ เพิ่ม: ฟังเหตุการณ์เมื่อกลับจาก RecordWorkoutFragment
        setupFragmentResultListener()

        // ✅ เพิ่ม: ฟังการเปลี่ยน Fragment เพื่อแสดง/ซ่อนเมนูอัตโนมัติ
        setupFragmentLifecycleListener()
    }

    /**
     * ✅ แสดง/ซ่อน BottomNavigation - ป้องกัน crash
     */
    fun setBottomNavVisible(isVisible: Boolean) {
        try {
            // ✅ เช็คว่า Fragment ยัง attached อยู่และ view ยังไม่ถูก destroy
            if (isAdded && view != null) {
                binding.bottomNavigation.visibility = if (isVisible) View.VISIBLE else View.GONE
                Log.d(TAG, "✅ BottomNavigation visibility: ${if (isVisible) "VISIBLE" else "GONE"}")
            } else {
                Log.w(TAG, "⚠️ Fragment not attached or view is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting BottomNavigation visibility: ${e.message}", e)
        }
    }

    /**
     * ✅ แสดง/ซ่อนปุ่มเมนูตารางซ้อม (เรียกจาก MainActivity <- HomeFragment)
     */
    fun updateScheduleMenuVisibility(isVisible: Boolean) {
        try {
            if (isAdded && view != null) {
                val menu = binding.bottomNavigation.menu
                val scheduleMenuItem = menu.findItem(R.id.bottom_calendar)

                if (scheduleMenuItem != null) {
                    scheduleMenuItem.isVisible = isVisible
                    Log.d(TAG, "📱 Schedule menu item visibility: $isVisible")
                } else {
                    Log.w(TAG, "⚠️ Schedule menu item (bottom_calendar) not found")
                }
            } else {
                Log.w(TAG, "⚠️ Fragment not attached or view is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating schedule menu visibility: ${e.message}", e)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        // ✅ ล้าง back stack เฉพาะเมื่อสลับเมนู
        childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        childFragmentManager.beginTransaction()
            .replace(binding.container.id, fragment)
            .commit()
    }

    // ✅ เพิ่มฟังก์ชันใหม่: รับสัญญาณจาก RecordWorkoutFragment
    private fun setupFragmentResultListener() {
        childFragmentManager.setFragmentResultListener(
            "workout_saved_return_to_schedule",
            this
        ) { _, bundle ->
            val weekNumber = bundle.getInt("week_number", 1)

            // กลับไปหน้า TrainingSchedule พร้อมสัปดาห์เดิม
            val fragment = TrainingScheduleFragment.newInstance(
                initialWeek = weekNumber,
                trainingPlanId = null
            )

            // ล้าง back stack
            childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

            childFragmentManager.beginTransaction()
                .replace(binding.container.id, fragment)
                .commit()

            // ตั้งค่า BottomNavigation ให้เลือกเมนูตารางซ้อม
            binding.bottomNavigation.selectedItemId = R.id.bottom_calendar
        }
    }

    // ✅ ฟังก์ชันใหม่: ตรวจจับการเปลี่ยน Fragment และแสดง/ซ่อนเมนูอัตโนมัติ
    private fun setupFragmentLifecycleListener() {
        childFragmentManager.addOnBackStackChangedListener {
            val currentFragment = childFragmentManager.findFragmentById(binding.container.id)

            // ✅ แสดงเมนูสำหรับหน้าหลัก 4 หน้า
            when (currentFragment) {
                is HomeFragment,
                is TrainingScheduleFragment,
                is ArticlesFragment,
                is ProfileFragment -> {
                    setBottomNavVisible(true)
                }
                else -> {
                    // ซ่อนเมนูสำหรับหน้าอื่นๆ (เช่น NewbieDetailsFragment, RunningGoal5kFragment)
                    setBottomNavVisible(false)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ เมื่อกลับมาที่ MainFragment ให้แสดงเมนูอัตโนมัติ
        val currentFragment = childFragmentManager.findFragmentById(binding.container.id)
        when (currentFragment) {
            is HomeFragment,
            is TrainingScheduleFragment,
            is ArticlesFragment,
            is ProfileFragment -> {
                setBottomNavVisible(true)
            }
        }
    }
}