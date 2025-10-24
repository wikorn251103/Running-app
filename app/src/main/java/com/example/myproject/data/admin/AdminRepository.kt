package com.example.myproject.data.admin

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()

    // ดึงรายชื่อนักกีฬาทั้งหมด (ไม่รวมแอดมิน)
    suspend fun getAllAthletes(): List<AthleteModel> {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("role", "user") // กรองเฉพาะผู้ใช้ทั่วไป
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(AthleteModel::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ดึงข้อมูลตารางซ้อมทั้งหมดจาก training_plans
    suspend fun getAllTrainingPlans(): List<ProgramModel> {
        return try {
            val snapshot = db.collection("training_plans").get().await()
            snapshot.documents.mapNotNull { doc ->
                ProgramModel(
                    id = doc.id,
                    name = doc.id,
                    pace = "",
                    type = "",
                    description = "",
                    week = ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ดึงข้อมูลตารางซ้อมของ athlete แต่ละคน
    suspend fun getTrainingPlan(planId: String): Map<String, Any>? {
        return try {
            val doc = db.collection("training_plans").document(planId).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            null
        }
    }

    // ดึงข้อมูลตารางซ้อมรายวันจาก week
    suspend fun getTrainingDaysByWeek(planId: String, weekNumber: Int): List<TrainingDayModel> {
        return try {
            val doc = db.collection("training_plans")
                .document(planId)
                .get()
                .await()

            if (doc.exists()) {
                val weekData = doc.get("week_$weekNumber") as? Map<String, Any>
                weekData?.entries?.mapNotNull { entry ->
                    val dayData = entry.value as? Map<String, Any>
                    TrainingDayModel(
                        id = entry.key,
                        day = dayData?.get("day") as? String ?: "",
                        description = dayData?.get("description") as? String ?: "",
                        pace = dayData?.get("pace") as? String ?: "",
                        type = dayData?.get("type") as? String ?: ""
                    )
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // อัพเดทตารางซ้อมของนักกีฬา
    suspend fun updateAthleteTrainingPlan(uid: String, planId: String): Boolean {
        return try {
            db.collection("users")
                .document(uid)
                .update("trainingPlan", planId)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // อัพเดทข้อมูลซ้อมรายวัน
    suspend fun updateTrainingDay(
        planId: String,
        weekNumber: Int,
        dayId: String,
        dayData: TrainingDayModel
    ): Boolean {
        return try {
            val updateMap = mapOf(
                "week_${weekNumber}.$dayId" to mapOf(
                    "day" to dayData.day,
                    "description" to dayData.description,
                    "pace" to dayData.pace,
                    "type" to dayData.type
                )
            )

            db.collection("training_plans")
                .document(planId)
                .update(updateMap)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ยกเลิกแผนของนักกีฬา
    suspend fun cancelAthletePlan(uid: String) {
        try {
            db.collection("users")
                .document(uid)
                .update("trainingPlan", "")
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}