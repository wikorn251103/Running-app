package com.example.myproject.Fragment.training

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class MissedWorkoutCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "MissedWorkoutCheck"
    }

    override suspend fun doWork(): Result {
        return try {
            // ดึง userId ของผู้ใช้ที่ล็อกอินอยู่
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.d(TAG, "⚠️ No user logged in, skipping check")
                return Result.success()
            }

            Log.d(TAG, "🔍 Starting daily missed workout check...")
            checkAllMissedDays(userId)

            Log.d(TAG, "✅ Missed workout check completed successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking missed workouts", e)
            Result.retry() // ลองใหม่ถ้าเกิด error
        }
    }

    /**
     * เช็คขาดซ้อมทุกสัปดาห์ (1-4) และทุกวัน (1-7)
     */
    private suspend fun checkAllMissedDays(userId: String) {
        val document = firestore.collection("Athletes")
            .document(userId)
            .get()
            .await()

        if (!document.exists()) {
            Log.d(TAG, "⚠️No athlete document found for user: $userId")
            return
        }

        // ตรวจสอบว่ามีโปรแกรมอยู่หรือไม่
        val isActive = document.getBoolean("isActive") ?: false
        if (!isActive) {
            Log.d(TAG, "⚠️ Program is not active, skipping check")
            return
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val programStartDate = document.getTimestamp("startDate")?.toDate()
        if (programStartDate == null) {
            Log.d(TAG, "⚠️ No program start date found")
            return
        }

        val programStart = Calendar.getInstance().apply {
            time = programStartDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var missedCount = 0

        // เช็คทุกสัปดาห์ (1-4)
        for (week in 1..4) {
            val weekData = document.get("week_$week") as? HashMap<*, *>
            if (weekData == null) {
                Log.d(TAG, "⚠️ No data found for week $week")
                continue
            }

            // เช็คทุกวัน (1-7)
            for (day in 1..7) {
                val dayData = weekData["day_$day"] as? HashMap<*, *>
                if (dayData == null) {
                    Log.d(TAG, "⚠️ No data found for week $week day $day")
                    continue
                }

                val isCompleted = dayData["isCompleted"] as? Boolean ?: false
                val isMissed = dayData["isMissed"] as? Boolean ?: false
                val type = dayData["type"] as? String ?: ""

                // คำนวณวันที่ของวันนั้นๆ
                val dayDate = Calendar.getInstance().apply {
                    time = programStart.time
                    add(Calendar.DAY_OF_YEAR, ((week - 1) * 7) + (day - 1))
                }

                // เงื่อนไข: วันที่ผ่านไปแล้ว + ไม่ได้ซ้อม + ยังไม่ถูก mark + ไม่ใช่ Rest Day
                if (dayDate.before(today) &&
                    !isCompleted &&
                    !isMissed &&
                    !type.equals("Rest Day", ignoreCase = true)) {

                    try {
                        // Mark as missed
                        val fieldPath = "week_$week.day_$day.isMissed"
                        firestore.collection("Athletes")
                            .document(userId)
                            .update(fieldPath, true)
                            .await()

                        missedCount++
                        Log.d(TAG, "❌ Marked as missed: Week $week, Day $day ($type)")

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to mark week $week day $day as missed", e)
                    }
                }
            }
        }

        if (missedCount > 0) {
            Log.d(TAG, "📊 Total missed workouts marked: $missedCount")
        } else {
            Log.d(TAG, "✅ No missed workouts found")
        }
    }
}