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

    // ✅ Pace Feedback — เพิ่มใหม่
    val paceResult: String = "",        // "faster" | "on_target" | "slower_slight" | "slower_much"
    val paceDiffSeconds: Int = 0,       // ต่างจากเป้ากี่วินาที/กม.

    // สถานะ
    val isCompleted: Boolean = true
) {
    fun calculateAveragePace(): String {
        if (actualDistance <= 0 || actualDuration <= 0) return "0:00"
        val paceInSeconds = (actualDuration / actualDistance).toInt()
        return String.format("%d:%02d", paceInSeconds / 60, paceInSeconds % 60)
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

    // ✅ Emoji สำหรับแสดงในการ์ดประวัติ
    fun getPaceEmoji(): String = when (paceResult) {
        "faster"        -> "🏆"
        "on_target"     -> "🎯"
        "slower_slight" -> "😅"
        "slower_much"   -> "😢"
        else            -> ""
    }

    // ✅ หัวข้อสำหรับ Dialog
    fun getPaceFeedbackTitle(): String = when (paceResult) {
        "faster"        -> "ยอดเยี่ยมมาก!"
        "on_target"     -> "ได้เป้าหมาย!"
        "slower_slight" -> "ช้ากว่าเป้าเล็กน้อย"
        "slower_much"   -> "ต้องพัฒนาเพิ่มขึ้น"
        else            -> "ผลการซ้อม"
    }

    // ✅ ข้อความสำหรับ Dialog
    fun getPaceFeedbackMessage(): String = when (paceResult) {
        "faster"        ->
            "คุณวิ่งได้เร็วกว่าเป้าหมาย $paceDiffSeconds วินาที/กม.\n" +
                    "ฟอร์มและความแข็งแกร่งของคุณดีมาก 💪\nรักษาระดับนี้ต่อไปนะ!"
        "on_target"     ->
            "เพซของคุณอยู่ในช่วงที่กำหนดพอดี ✅\n" +
                    "การควบคุมจังหวะของคุณดีมาก\nรักษาฟอร์มนี้ต่อไปเลย!"
        "slower_slight" ->
            "ช้ากว่าเป้าหมาย $paceDiffSeconds วินาที/กม.\n" +
                    "ลองเพิ่มความถี่ก้าว (Cadence) ดูนะ\nแค่นิดเดียวก็จะถึงเป้าแล้ว!"
        "slower_much"   ->
            "ช้ากว่าเป้าหมายถึง $paceDiffSeconds วินาที/กม.\n" +
                    "ไม่ต้องกังวล ทุกคนพัฒนาได้!\nลองดูคลิปเทคนิคการวิ่งด้านล่างนะ 📺"
        else            -> "ไม่มีข้อมูลเพซ"
    }
}