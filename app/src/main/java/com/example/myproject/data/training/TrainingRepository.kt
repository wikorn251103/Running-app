package com.example.myproject.data.training

import com.google.firebase.firestore.FirebaseFirestore

class TrainingRepository(private val firestore: FirebaseFirestore) {
    fun getTrainingWeekData(
        planId: String,
        week: Int,
        onSuccess: (List<TrainingModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("training_plans")
            .document(planId)
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
                            )
                            trainingDays.add(trainingDay)
                        }
                    }
                    onSuccess(trainingDays)
                } else {
                    onFailure(Exception("ไม่พบแผนซ้อม $planId"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}


