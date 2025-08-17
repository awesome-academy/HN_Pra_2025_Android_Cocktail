package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.CocktailTable
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CocktailFirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val cocktailsCollection = firestore.collection("cocktails")

    // Create
    suspend fun createCocktail(cocktail: CocktailTable): Result<String> {
        return try {
            val cocktailId = if (cocktail.id.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                cocktail.id
            }
            
            val newCocktail = cocktail.copy(
                id = cocktailId,
                updatedAt = System.currentTimeMillis()
            )
            
            cocktailsCollection.document(cocktailId).set(newCocktail).await()
            Result.success(cocktailId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Read
    suspend fun getCocktail(cocktailId: String): Result<CocktailTable?> {
        return try {
            val document = cocktailsCollection.document(cocktailId).get().await()
            if (document.exists()) {
                val cocktail = document.toObject(CocktailTable::class.java)
                Result.success(cocktail)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCocktails(): Result<List<CocktailTable>> {
        return try {
            val querySnapshot = cocktailsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val cocktails = querySnapshot.toObjects(CocktailTable::class.java)
            Result.success(cocktails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCocktails(searchQuery: String): Result<List<CocktailTable>> {
        return try {
            // Firebase doesn't support full-text search, so we'll do multiple queries
            val nameResults = cocktailsCollection
                .whereGreaterThanOrEqualTo("name", searchQuery)
                .whereLessThanOrEqualTo("name", searchQuery + "\uf8ff")
                .get().await()

            val categoryResults = cocktailsCollection
                .whereEqualTo("category", searchQuery)
                .get().await()

            val cocktails = mutableSetOf<CocktailTable>()
            cocktails.addAll(nameResults.toObjects(CocktailTable::class.java))
            cocktails.addAll(categoryResults.toObjects(CocktailTable::class.java))

            Result.success(cocktails.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCocktailsByCategory(category: String): Result<List<CocktailTable>> {
        return try {
            val querySnapshot = cocktailsCollection
                .whereEqualTo("category", category)
                .orderBy("name")
                .get().await()
            val cocktails = querySnapshot.toObjects(CocktailTable::class.java)
            Result.success(cocktails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCocktailsByAlcoholic(alcoholic: String): Result<List<CocktailTable>> {
        return try {
            val querySnapshot = cocktailsCollection
                .whereEqualTo("alcoholic", alcoholic)
                .orderBy("name")
                .get().await()
            val cocktails = querySnapshot.toObjects(CocktailTable::class.java)
            Result.success(cocktails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserCreatedCocktails(uid: String): Result<List<CocktailTable>> {
        return try {
            val querySnapshot = cocktailsCollection
                .whereEqualTo("createdBy", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val cocktails = querySnapshot.toObjects(CocktailTable::class.java)
            Result.success(cocktails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularCocktails(limit: Int = 20): Result<List<CocktailTable>> {
        return try {
            val querySnapshot = cocktailsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
            val cocktails = querySnapshot.toObjects(CocktailTable::class.java)
            Result.success(cocktails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRandomCocktails(limit: Int = 10): Result<List<CocktailTable>> {
        return try {
            // Since Firestore doesn't have random queries, we'll get all and shuffle
            val querySnapshot = cocktailsCollection.get().await()
            val allCocktails = querySnapshot.toObjects(CocktailTable::class.java)
            val randomCocktails = allCocktails.shuffled().take(limit)
            Result.success(randomCocktails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update
    suspend fun updateCocktail(cocktail: CocktailTable): Result<Boolean> {
        return try {
            val updatedCocktail = cocktail.copy(updatedAt = System.currentTimeMillis())
            cocktailsCollection.document(cocktail.id).set(updatedCocktail).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete
    suspend fun deleteCocktail(cocktailId: String): Result<Boolean> {
        return try {
            cocktailsCollection.document(cocktailId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Batch operations
    suspend fun createMultipleCocktails(cocktails: List<CocktailTable>): Result<List<String>> {
        return try {
            val batch = firestore.batch()
            val cocktailIds = mutableListOf<String>()

            for (cocktail in cocktails) {
                val cocktailId = if (cocktail.id.isEmpty()) {
                    UUID.randomUUID().toString()
                } else {
                    cocktail.id
                }
                
                val newCocktail = cocktail.copy(
                    id = cocktailId,
                    updatedAt = System.currentTimeMillis()
                )
                
                val docRef = cocktailsCollection.document(cocktailId)
                batch.set(docRef, newCocktail)
                cocktailIds.add(cocktailId)
            }

            batch.commit().await()
            Result.success(cocktailIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check if cocktail exists
    suspend fun cocktailExists(cocktailId: String): Result<Boolean> {
        return try {
            val document = cocktailsCollection.document(cocktailId).get().await()
            Result.success(document.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
