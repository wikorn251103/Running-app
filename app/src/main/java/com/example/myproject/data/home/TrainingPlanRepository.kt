package com.example.myproject.data.home

import com.example.myproject.data.training.TrainingModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TrainingPlanRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("training_plans")

    suspend fun getTrainingPlan(planId: String): Map<String, Map<String, TrainingModel>> {
        val snapshot = collection.document(planId).get().await()
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