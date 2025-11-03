package com.example.myproject.data.workout

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WorkoutRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "WorkoutRepository"
        private const val COLLECTION_WORKOUT_HISTORY = "workoutHistory"
        private const val COLLECTION_ATHLETES = "Athletes"
    }

    /**
     * บันทึกการซ้อม
     */
    suspend fun saveWorkoutLog(workoutLog: WorkoutLog): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            val logId = workoutLog.logId.ifEmpty {
                firestore.collection(COLLECTION_WORKOUT_HISTORY)
                    .document(userId)
                    .collection("logs")
                    .document().id
            }

            val updatedLog = workoutLog.copy(
                logId = logId,
                userId = userId
            )

            firestore.collection(COLLECTION_WORKOUT_HISTORY)
                .document(userId)
                .collection("logs")
                .document(logId)
                .set(updatedLog)
                .await()

            Log.d(TAG, "✅ Workout log saved: $logId")
            Result.success(logId)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving workout log", e)
            Result.failure(e)
        }
    }

    /**
     * ⭐ อัปเดท isCompleted = true ใน Athletes/{userId}/week_X/day_Y
     */
    suspend fun markDayAsCompleted(weekNumber: Int, dayNumber: Int): Result<Boolean> {
        return suspendCoroutine { continuation ->
            val userId = auth.currentUser?.uid
            if (userId == null) {
                continuation.resume(Result.failure(Exception("User not logged in")))
                return@suspendCoroutine
            }

            val fieldPath = "week_$weekNumber.day_$dayNumber.isCompleted"

            firestore.collection(COLLECTION_ATHLETES)
                .document(userId)
                .update(fieldPath, true)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Day marked as completed: Week $weekNumber, Day $dayNumber")
                    continuation.resume(Result.success(true))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error marking day as completed", e)
                    continuation.resume(Result.failure(e))
                }
        }
    }

    /**
     * ⭐ อัปเดท isMissed = true สำหรับวันที่ขาดซ้อม
     */
    suspend fun markDayAsMissed(weekNumber: Int, dayNumber: Int): Result<Boolean> {
        return suspendCoroutine { continuation ->
            val userId = auth.currentUser?.uid
            if (userId == null) {
                continuation.resume(Result.failure(Exception("User not logged in")))
                return@suspendCoroutine
            }

            val fieldPath = "week_$weekNumber.day_$dayNumber.isMissed"

            firestore.collection(COLLECTION_ATHLETES)
                .document(userId)
                .update(fieldPath, true)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Day marked as missed: Week $weekNumber, Day $dayNumber")
                    continuation.resume(Result.success(true))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error marking day as missed", e)
                    continuation.resume(Result.failure(e))
                }
        }
    }

    /**
     * ดึงประวัติการซ้อมทั้งหมด
     */
    suspend fun getAllWorkoutLogs(): Result<List<WorkoutLog>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            val snapshot = firestore.collection(COLLECTION_WORKOUT_HISTORY)
                .document(userId)
                .collection("logs")
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkoutLog::class.java)
            }

            Log.d(TAG, "✅ Retrieved ${logs.size} workout logs")
            Result.success(logs)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting workout logs", e)
            Result.failure(e)
        }
    }

    /**
     * ดึงประวัติการซ้อมตามช่วงเวลา
     */
    suspend fun getWorkoutLogsByDateRange(startTime: Long, endTime: Long): Result<List<WorkoutLog>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            val snapshot = firestore.collection(COLLECTION_WORKOUT_HISTORY)
                .document(userId)
                .collection("logs")
                .whereGreaterThanOrEqualTo("completedAt", startTime)
                .whereLessThanOrEqualTo("completedAt", endTime)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkoutLog::class.java)
            }

            Log.d(TAG, "✅ Retrieved ${logs.size} workout logs for date range")
            Result.success(logs)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting workout logs by date range", e)
            Result.failure(e)
        }
    }

    /**
     * ดึงประวัติการซ้อมของสัปดาห์นั้นๆ
     */
    suspend fun getWorkoutLogsByWeek(weekNumber: Int): Result<List<WorkoutLog>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            val snapshot = firestore.collection(COLLECTION_WORKOUT_HISTORY)
                .document(userId)
                .collection("logs")
                .whereEqualTo("weekNumber", weekNumber)
                .orderBy("dayNumber", Query.Direction.ASCENDING)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkoutLog::class.java)
            }

            Log.d(TAG, "✅ Retrieved ${logs.size} workout logs for week $weekNumber")
            Result.success(logs)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting workout logs by week", e)
            Result.failure(e)
        }
    }

    /**
     * ลบ workout log
     */
    suspend fun deleteWorkoutLog(logId: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            firestore.collection(COLLECTION_WORKOUT_HISTORY)
                .document(userId)
                .collection("logs")
                .document(logId)
                .delete()
                .await()

            Log.d(TAG, "✅ Workout log deleted: $logId")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting workout log", e)
            Result.failure(e)
        }
    }

    /**
     * คำนวณสถิติรวม
     */
    suspend fun getWorkoutStatistics(): Result<WorkoutStatistics> {
        return try {
            val logsResult = getAllWorkoutLogs()
            if (logsResult.isFailure) {
                return Result.failure(logsResult.exceptionOrNull() ?: Exception("Failed to get logs"))
            }

            val logs = logsResult.getOrNull() ?: emptyList()

            val stats = WorkoutStatistics(
                totalWorkouts = logs.size,
                totalDistance = logs.sumOf { it.actualDistance },
                totalDuration = logs.sumOf { it.actualDuration },
                totalCalories = logs.sumOf { it.calories },
                averagePace = calculateAveragePace(logs),
                longestRun = logs.maxByOrNull { it.actualDistance }?.actualDistance ?: 0.0,
                fastestPace = findFastestPace(logs)
            )

            Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calculating statistics", e)
            Result.failure(e)
        }
    }

    private fun calculateAveragePace(logs: List<WorkoutLog>): String {
        if (logs.isEmpty()) return "0:00"

        val totalDistance = logs.sumOf { it.actualDistance }
        val totalDuration = logs.sumOf { it.actualDuration }

        if (totalDistance <= 0) return "0:00"

        val avgPaceSeconds = (totalDuration / totalDistance).toInt()
        val minutes = avgPaceSeconds / 60
        val seconds = avgPaceSeconds % 60

        return String.format("%d:%02d", minutes, seconds)
    }

    private fun findFastestPace(logs: List<WorkoutLog>): String {
        if (logs.isEmpty()) return "0:00"

        val fastestLog = logs.filter { it.actualDistance > 0 && it.actualDuration > 0 }
            .minByOrNull { it.actualDuration / it.actualDistance }

        return fastestLog?.calculateAveragePace() ?: "0:00"
    }
}