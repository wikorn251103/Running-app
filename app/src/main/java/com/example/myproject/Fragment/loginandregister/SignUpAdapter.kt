package com.example.myproject.Fragment.loginandregister

import com.example.myproject.data.signup.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SignUpAdapter(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun signUpUser(
        name: String,
        email: String,
        password: String,
        height: Int,
        weight: Double,
        age: Int,
        gender: String,
    ): Result<Unit> {
        return try {
            // สมัครสมาชิก Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Result.failure(Exception("ไม่พบ UID"))

            // สร้าง UserModel ตรง ๆ
            val user = UserModel(
                uid = uid,
                name = name,
                email = email,
                height = height,
                weight = weight,
                age = age,
                gender = gender,
                trainingPlan = "",
                role = "user"// กำหนดค่าเริ่มต้นได้
            )

            // ✅ บันทึกลง Firestore โดยตรง
            db.collection("users").document(uid).set(user).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}