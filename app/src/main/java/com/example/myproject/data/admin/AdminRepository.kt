package com.example.myproject.data.admin

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminRepository {

    private val db = FirebaseFirestore.getInstance()

    // ดึงรายชื่อนักกีฬาทั้งหมด
    suspend fun getAllAthletes(): List<AthleteModel> {
        return try {
            val snapshot = db.collection("users").get().await()
            snapshot.documents.mapNotNull { it.toObject(AthleteModel::class.java) }
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

    // ยกเลิกแผนของนักกีฬา
    suspend fun cancelAthletePlan(uid: String) {
        try {
            db.collection("users").document(uid).update("trainingPlan", "").await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}