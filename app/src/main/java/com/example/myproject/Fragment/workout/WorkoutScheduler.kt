package com.example.myproject.Fragment.workout

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * ⭐ ตั้งเวลาเช็คการซ้อมรายวัน
 */
object WorkoutScheduler {

    private const val TAG = "WorkoutScheduler"
    private const val WORK_NAME = "daily_workout_check"

    /**
     * เริ่มต้น Daily Checker
     * - เช็คทุกวันเวลา 09:00 น. (แจ้งเตือนให้บันทึก)
     * - เช็คทุกวันเวลา 23:00 น. (เช็คว่าบันทึกหรือยัง ถ้าไม่บันทึก → ขาดซ้อม)
     */
    fun scheduleDailyCheck(context: Context) {
        // ยกเลิก schedule เก่า
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

        // คำนวณเวลาที่จะรันครั้งแรก (เช้า 9 โมง)
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // ถ้าเกิน 9 โมงแล้ว ให้รันพรุ่งนี้
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        // สร้าง PeriodicWorkRequest (รันทุก 12 ชั่วโมง)
        val workRequest = PeriodicWorkRequestBuilder<WorkoutTrackerService>(
            12, TimeUnit.HOURS // เช็ค 2 ครั้งต่อวัน (9:00 และ 21:00)
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // ต้องมี Internet
                    .build()
            )
            .addTag("workout_reminder")
            .build()

        // Schedule
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "✅ Daily workout check scheduled (every 12 hours)")
    }

    /**
     * หยุด Daily Checker
     */
    fun cancelDailyCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "❌ Daily workout check cancelled")
    }

    /**
     * เช็คทันที (สำหรับ Testing)
     */
    fun runImmediateCheck(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<WorkoutTrackerService>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "🔄 Immediate check triggered")
    }
}