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
            // ✅ สร้าง account ใน Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("ไม่พบ UID กรุณาลองใหม่อีกครั้ง"))

            // ✅ ส่ง Verification Email ทันที
            authResult.user?.sendEmailVerification()?.await()

            // ✅ บันทึกข้อมูลลง Firestore
            val user = UserModel(
                uid = uid,
                name = name,
                email = email,
                height = height,
                weight = weight,
                age = age,
                gender = gender,
                trainingPlan = "",
                role = "user"
            )

            db.collection("users").document(uid).set(user).await()

            Result.success(Unit)
        } catch (e: Exception) {
            // ✅ ถ้า Firestore ล้มเหลวหลัง Auth สำเร็จ → ลบ account ออกด้วยเพื่อความสะอาด
            auth.currentUser?.delete()?.await()
            Result.failure(e)
        }
    }
}