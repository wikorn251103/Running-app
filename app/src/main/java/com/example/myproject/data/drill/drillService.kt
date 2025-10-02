package com.example.myproject.data.drill

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface drillService {
    fun getdrillsFlow(): Flow<List<drillModel>>
}

class drillServiceImpl : drillService {
    override fun getdrillsFlow(): Flow<List<drillModel>> = callbackFlow {
        val db = Firebase.firestore
        val listener = db.collection("drills_run")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val drills = snapshots?.documents?.mapNotNull {
                    it.toObject(drillModel::class.java)
                } ?: emptyList()

                trySend(drills)
            }

        awaitClose {
            listener.remove()
        }
    }
}