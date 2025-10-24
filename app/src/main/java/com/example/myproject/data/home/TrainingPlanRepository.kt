package com.example.myproject.data.training

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TrainingPlanRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * ⭐ ดึงข้อมูลจาก Athletes/{userId} แทน training_plans
     */
    suspend fun getTrainingPlanFromAthlete(): Map<String, Map<String, TrainingModel>> {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            throw Exception("กรุณา Login ก่อน")
        }

        val snapshot = db.collection("Athletes")
            .document(userId)
            .get()
            .await()

        val data = snapshot.data ?: return emptyMap()

        val weeks = mutableMapOf<String, Map<String, TrainingModel>>()

        // วนลูปหาทุก week_X
        for ((key, value) in data) {
            if (key.startsWith("week_") && value is Map<*, *>) {
                val days = mutableMapOf<String, TrainingModel>()

                // วนลูปหาทุก day_X ในแต่ละ week
                for ((dayKey, dayValue) in value) {
                    if (dayKey.toString().startsWith("day_") && dayValue is Map<*, *>) {
                        val trainingDay = TrainingModel(
                            day = dayValue["day"] as? String ?: "",
                            description = dayValue["description"] as? String ?: "",
                            pace = dayValue["pace"] as? String ?: "",
                            type = dayValue["type"] as? String ?: ""
                        )
                        days[dayKey.toString()] = trainingDay
                    }
                }
                weeks[key] = days
            }
        }

        return weeks
    }

    /**
     * เก่า - ดึงจาก training_plans (เผื่อต้องการใช้)
     */
    suspend fun getTrainingPlan(planId: String): Map<String, Map<String, TrainingModel>> {
        val snapshot = db.collection("training_plans")
            .document(planId)
            .get()
            .await()

        val data = snapshot.data ?: return emptyMap()

        val weeks = mutableMapOf<String, Map<String, TrainingModel>>()
        for ((weekKey, weekValue) in data) {
            if (weekValue is Map<*, *>) {
                val days = mutableMapOf<String, TrainingModel>()
                for ((dayKey, dayValue) in weekValue) {
                    if (dayValue is Map<*, *>) {
                        val trainingDay = TrainingModel(
                            day = dayValue["day"] as? String ?: "",
                            description = dayValue["description"] as? String ?: "",
                            pace = dayValue["pace"] as? String ?: "",
                            type = dayValue["type"] as? String ?: ""
                        )
                        days[dayKey.toString()] = trainingDay
                    }
                }
                weeks[weekKey] = days
            }
        }
        return weeks
    }
}