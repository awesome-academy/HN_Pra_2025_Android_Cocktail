package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.Checkmark
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CheckmarkFirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val checkmarksCollection = firestore.collection("checkmarks")

    suspend fun addCheckmark(uid: String, cocktailId: String): Result<String> {
        return try {
            val existing = isCheckmarked(uid, cocktailId)
            if (existing.isSuccess && existing.getOrNull() == true) {
                return Result.failure(Exception("Cocktail already checkmarked by this user"))
            }

            val checkmarkId = UUID.randomUUID().toString()
            val checkmark = Checkmark(
                id = checkmarkId,
                uid = uid,
                cocktailId = cocktailId
            )
            checkmarksCollection.document(checkmarkId).set(checkmark).await()
            Result.success(checkmarkId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserCheckmarks(uid: String): Result<List<Checkmark>> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .orderBy("checkedAt", Query.Direction.DESCENDING)
                .get().await()
            val checkmarks = querySnapshot.toObjects(Checkmark::class.java)
            Result.success(checkmarks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCocktailCheckmarks(cocktailId: String): Result<List<Checkmark>> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            val checkmarks = querySnapshot.toObjects(Checkmark::class.java)
            Result.success(checkmarks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCheckmarkCount(cocktailId: String): Result<Int> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            Result.success(querySnapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isCheckmarked(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            Result.success(!querySnapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCheckmark(uid: String, cocktailId: String): Result<Checkmark?> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            
            if (!querySnapshot.isEmpty) {
                val checkmark = querySnapshot.documents[0].toObject(Checkmark::class.java)
                Result.success(checkmark)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCheckmark(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            
            for (document in querySnapshot.documents) {
                document.reference.delete().await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCheckmarkById(checkmarkId: String): Result<Boolean> {
        return try {
            checkmarksCollection.document(checkmarkId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCheckmark(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            val isCurrentlyChecked = isCheckmarked(uid, cocktailId)
            if (isCurrentlyChecked.isSuccess && isCurrentlyChecked.getOrNull() == true) {
                removeCheckmark(uid, cocktailId)
                Result.success(false)
            } else {
                addCheckmark(uid, cocktailId)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentCheckmarks(uid: String, limit: Int = 10): Result<List<Checkmark>> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .orderBy("checkedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
            val checkmarks = querySnapshot.toObjects(Checkmark::class.java)
            Result.success(checkmarks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearUserCheckmarks(uid: String): Result<Boolean> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .get().await()
            
            for (document in querySnapshot.documents) {
                document.reference.delete().await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
