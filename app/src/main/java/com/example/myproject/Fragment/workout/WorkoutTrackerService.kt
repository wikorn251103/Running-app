package com.example.myproject.Fragment.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Worker สำหรับเช็คการซ้อมรายวัน
 * - เช็คว่าวันนี้มีตารางซ้อมหรือไม่
 * - ถ้ามี แจ้งเตือนให้ไปบันทึก
 * - ถ้าไม่บันทึกภายในเวลา → ขาดซ้อม
 */
class WorkoutTrackerService(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "WorkoutTrackerService"
        private const val CHANNEL_ID = "workout_reminder_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔔 Daily workout check started")

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "⚠️ User not logged in")
            return Result.success()
        }

        try {
            // 1. ดึงข้อมูลโปรแกรมปัจจุบัน
            val athleteDoc = firestore.collection("Athletes")
                .document(userId)
                .get()
                .await()

            if (!athleteDoc.exists()) {
                Log.w(TAG, "⚠️ No active program")
                return Result.success()
            }

            val isActive = athleteDoc.getBoolean("isActive") ?: false
            if (!isActive) {
                Log.w(TAG, "⚠️ Program not active")
                return Result.success()
            }

            // 2. หาว่าวันนี้วันไหน
            val today = getTodayDayName()
            val todayDayNumber = getTodayDayNumber()

            Log.d(TAG, "📅 Today is: $today (Day $todayDayNumber)")

            // 3. หาว่าตอนนี้สัปดาห์ไหน (จาก createdAt)
            val createdAt = athleteDoc.getLong("createdAt") ?: System.currentTimeMillis()
            val currentWeek = calculateCurrentWeek(createdAt)

            Log.d(TAG, "📊 Current week: $currentWeek")

            // 4. ดึงข้อมูลวันนี้จากตาราง
            val weeks = athleteDoc.get("weeks") as? Map<*, *>
            val weekData = weeks?.get("week$currentWeek") as? Map<*, *>
            val todayData = weekData?.get("day$todayDayNumber") as? Map<*, *>

            if (todayData == null) {
                Log.d(TAG, "ℹ️ No training scheduled for today")
                return Result.success()
            }

            // 5. เช็คว่าทำแล้วหรือยัง
            val isCompleted = todayData["isCompleted"] as? Boolean ?: false
            val isMissed = todayData["isMissed"] as? Boolean ?: false

            if (isCompleted) {
                Log.d(TAG, "✅ Already completed today")
                return Result.success()
            }

            if (isMissed) {
                Log.d(TAG, "❌ Already marked as missed")
                return Result.success()
            }

            // 6. เช็คเวลา - ถ้าเกินเที่ยงคืนแล้ว → ขาดซ้อม
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            if (currentHour >= 23) {
                // เกือบหมดวันแล้ว ยังไม่บันทึก → ขาดซ้อม
                markAsMissed(userId, currentWeek, todayDayNumber)
                Log.d(TAG, "❌ Marked as missed - past deadline")
                return Result.success()
            }

            // 7. ยังไม่หมดเวลา → แจ้งเตือนให้บันทึก
            val trainingType = todayData["type"] as? String ?: "ซ้อม"
            val description = todayData["description"] as? String ?: ""

            sendReminderNotification(trainingType, description)
            Log.d(TAG, "🔔 Reminder sent")

            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in workout tracker", e)
            return Result.failure()
        }
    }

    /**
     * หาว่าวันนี้วันอะไร (Monday, Tuesday, ...)
     */
    private fun getTodayDayName(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> ""
        }
    }

    /**
     * หาว่าวันนี้วันที่เท่าไหร่ในสัปดาห์ (1-7)
     */
    private fun getTodayDayNumber(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    /**
     * คำนวณว่าตอนนี้อยู่สัปดาห์ไหน
     */
    private fun calculateCurrentWeek(programStartTime: Long): Int {
        val now = System.currentTimeMillis()
        val daysPassed = ((now - programStartTime) / (1000 * 60 * 60 * 24)).toInt()
        val weeksPassed = daysPassed / 7
        return (weeksPassed + 1).coerceIn(1, 4) // จำกัดไว้ที่สัปดาห์ 1-4
    }

    /**
     * ⭐ ทำเครื่องหมายว่าขาดซ้อม
     */
    private suspend fun markAsMissed(userId: String, weekNumber: Int, dayNumber: Int) {
        try {
            val fieldPath = "weeks.week$weekNumber.day$dayNumber.isMissed"

            firestore.collection("Athletes")
                .document(userId)
                .update(fieldPath, true)
                .await()

            Log.d(TAG, "✅ Marked as missed: Week $weekNumber, Day $dayNumber")

            // แจ้งเตือนว่าขาดซ้อม
            sendMissedNotification()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark as missed", e)
        }
    }

    /**
     * 🔔 แจ้งเตือนให้บันทึกการซ้อม
     */
    private fun sendReminderNotification(trainingType: String, description: String) {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ ถึงเวลาซ้อมแล้ว!")
            .setContentText("$trainingType: $description")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("วันนี้คุณมีตารางซ้อม $trainingType\n$description\n\nอย่าลืมบันทึกผลการซ้อมนะครับ!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 🔔 แจ้งเตือนว่าขาดซ้อม
     */
    private fun sendMissedNotification() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("❌ คุณขาดซ้อมเมื่อวาน")
            .setContentText("อย่าลืมซ้อมในวันถัดไปนะครับ!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * สร้าง Notification Channel (สำหรับ Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "แจ้งเตือนการซ้อม"
            val descriptionText = "แจ้งเตือนเมื่อถึงเวลาซ้อมและเมื่อขาดซ้อม"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}