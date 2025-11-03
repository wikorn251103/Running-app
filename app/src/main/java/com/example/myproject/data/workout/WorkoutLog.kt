package com.example.myproject.data.workout

data class WorkoutLog(
    val logId: String = "",
    val userId: String = "",
    val programId: String = "",
    val weekNumber: Int = 0,
    val dayNumber: Int = 0,
    val dayName: String = "",

    // ข้อมูลการซ้อม
    val trainingType: String = "",
    val plannedDescription: String = "",
    val plannedPace: String = "",

    // ผลการซ้อม
    val actualDistance: Double = 0.0,
    val actualDuration: Long = 0,
    val actualPace: String = "",
    val calories: Int = 0,
    val averageHeartRate: Int = 0,

    // เวลา
    val completedAt: Long = System.currentTimeMillis(),
    val startTime: Long = 0,
    val endTime: Long = 0,

    // หมายเหตุ
    val notes: String = "",
    val feeling: String = "",
    val weatherCondition: String = "",

    // สถานะ
    val isCompleted: Boolean = true
) {
    fun calculateAveragePace(): String {
        if (actualDistance <= 0 || actualDuration <= 0) return "0:00"

        val paceInSeconds = (actualDuration / actualDistance).toInt()
        val minutes = paceInSeconds / 60
        val seconds = paceInSeconds % 60

        return String.format("%d:%02d", minutes, seconds)
    }

    fun getFormattedDuration(): String {
        val hours = actualDuration / 3600
        val minutes = (actualDuration % 3600) / 60
        val seconds = actualDuration % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
}