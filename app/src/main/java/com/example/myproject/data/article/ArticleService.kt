package com.example.myproject.data.article

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface ArticleService {
    fun getArticlesFlow(): Flow<List<ArticleModel>>
}

class ArticleServiceImpl : ArticleService {
    override fun getArticlesFlow(): Flow<List<ArticleModel>> = callbackFlow {
        val db = Firebase.firestore
        val listener = db.collection("articles")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val articles = snapshots?.documents?.mapNotNull {
                    it.toObject(ArticleModel::class.java)
                } ?: emptyList()

                trySend(articles)
            }

        awaitClose {
            listener.remove()
        }
    }
}
