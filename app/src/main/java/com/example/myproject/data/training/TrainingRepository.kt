package com.example.myproject.data.training

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class TrainingRepository(private val firestore: FirebaseFirestore) {

    private val auth = FirebaseAuth.getInstance()
    private var weekListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "TrainingRepository"
    }

    /**
     * ⭐ ดึงข้อมูลแบบ Real-time จาก Athletes/{userId}
     */
    fun getTrainingWeekDataRealtime(
        week: Int,
        onSuccess: (List<TrainingModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration? {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onFailure(Exception("กรุณา Login ก่อน"))
            return null
        }

        Log.d(TAG, "🔄 Setting up real-time listener for week $week")

        // ✅ ใช้ addSnapshotListener แทน get()
        weekListener = firestore.collection("Athletes")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Listener error: ${error.message}", error)
                    onFailure(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val weekData = snapshot.get("week_$week") as? HashMap<*, *>

                    if (weekData == null) {
                        onFailure(Exception("ไม่พบข้อมูลสัปดาห์ที่ $week"))
                        return@addSnapshotListener
                    }

                    val trainingDays = mutableListOf<TrainingModel>()

                    for (i in 1..7) {
                        val dayData = weekData["day_$i"] as? HashMap<*, *>
                        dayData?.let {
                            val trainingDay = TrainingModel(
                                day = i.toString(),
                                description = it["description"] as? String ?: "",
                                pace = it["pace"] as? String ?: "",
                                type = it["type"] as? String ?: "",
                                isCompleted = it["isCompleted"] as? Boolean ?: false // ✅ เพิ่ม
                            )
                            trainingDays.add(trainingDay)
                        }
                    }

                    Log.d(TAG, "✅ Real-time update received: ${trainingDays.size} days")
                    onSuccess(trainingDays)
                } else {
                    onFailure(Exception("ไม่พบข้อมูลผู้ใช้"))
                }
            }

        return weekListener
    }

    /**
     * ยกเลิก Listener เมื่อไม่ใช้งาน
     */
    fun removeListener() {
        weekListener?.remove()
        weekListener = null
        Log.d(TAG, "🔕 Listener removed")
    }

    /**
     * เก่า - ดึงข้อมูลแบบปกติ (ไม่ real-time)
     */
    fun getTrainingWeekData(
        planId: String,
        week: Int,
        onSuccess: (List<TrainingModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onFailure(Exception("กรุณา Login ก่อน"))
            return
        }

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val weekData = document.get("week_$week") as? HashMap<*, *>

                    if (weekData == null) {
                        onFailure(Exception("ไม่พบข้อมูลสัปดาห์ที่ $week"))
                        return@addOnSuccessListener
                    }

                    val trainingDays = mutableListOf<TrainingModel>()

                    for (i in 1..7) {
                        val dayData = weekData["day_$i"] as? HashMap<*, *>
                        dayData?.let {
                            val trainingDay = TrainingModel(
                                day = i.toString(),
                                description = it["description"] as? String ?: "",
                                pace = it["pace"] as? String ?: "",
                                type = it["type"] as? String ?: "",
                                isCompleted = it["isCompleted"] as? Boolean ?: false
                            )
                            trainingDays.add(trainingDay)
                        }
                    }
                    onSuccess(trainingDays)
                } else {
                    onFailure(Exception("ไม่พบข้อมูลผู้ใช้ กรุณาเลือกโปรแกรมก่อน"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}