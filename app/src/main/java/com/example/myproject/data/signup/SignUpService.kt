package com.example.myproject.data.signup

import com.example.myproject.data.article.ArticleModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface SignUpService {
    fun getSignUpFlow(): Flow<List<UserModel>>
}

class SignUpServiceImpl : SignUpService {
    override fun getSignUpFlow(): Flow<List<UserModel>> = callbackFlow {
        val db = Firebase.firestore
        val listener = db.collection("users")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshots?.documents?.mapNotNull {
                    it.toObject(UserModel::class.java)
                } ?: emptyList()

                trySend(users)
            }

        awaitClose {
            listener.remove()
        }
    }
}