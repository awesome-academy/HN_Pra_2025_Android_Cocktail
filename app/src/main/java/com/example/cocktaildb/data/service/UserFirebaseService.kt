package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class UserFirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun createUser(user: User): Result<String> {
        return try {
            val docRef = usersCollection.document(user.uid)
            docRef.set(user).await()
            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val querySnapshot = usersCollection.get().await()
            val users = querySnapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersByEmail(email: String): Result<List<User>> {
        return try {
            val querySnapshot = usersCollection
                .whereEqualTo("email", email)
                .get().await()
            val users = querySnapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Boolean> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(uid: String, name: String, profileImage: String?): Result<Boolean> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "name" to name
            )
            profileImage?.let { updates["profileImage"] = it }

            usersCollection.document(uid).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(uid: String): Result<Boolean> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun userExists(uid: String): Result<Boolean> {
        return try {
            val document = usersCollection.document(uid).get().await()
            Result.success(document.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
