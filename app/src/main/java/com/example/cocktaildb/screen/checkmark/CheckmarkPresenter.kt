package com.example.cocktaildb.screen.checkmark

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class CheckmarkPresenter(
    private val context: Context,
    private val cocktailRepository: CocktailRepository
) : CheckmarkContract.Presenter {

    private var view: CheckmarkContract.View? = null
    private val auth = FirebaseAuth.getInstance()

    private val presenterJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + presenterJob)

    private val TAG = "CheckmarkPresenter"

    private var cachedCheckmarks: List<Cocktail> = emptyList()
    private var isLoading = false
    private var isProcessingCheckmarks = false

    companion object {
        private const val CHECKMARKS_PREFS = "checkmarks_prefs"
        private const val CHECKMARKS_KEY = "checkmarks_list"
        private const val MAX_CHECKMARKS_ITEMS = 100
        private const val SEPARATOR = "|||"
        private const val FIELD_SEPARATOR = ":::"
    }

    override fun setView(view: CheckmarkContract.View?) {
        this.view = view
    }

    override fun onStart() {
    }

    override fun onStop() {
        presenterJob.cancel()
    }

    override fun loadCheckmarks() {
        if (isLoading) return
        isLoading = true
        view?.displayLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null || !isNetworkAvailable()) {
            Log.d(TAG, "loadCheckmarks: No user or no network, loading from local only")
            loadFromLocalOnly()
            return
        }

        Log.d(TAG, "loadCheckmarks: Syncing Firebase with local for user ${currentUser.uid}")
        syncFirebaseWithLocal(currentUser.uid)
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun syncFirebaseWithLocal(userId: String) {
        uiScope.launch {
            try {
                val firebaseResult = withContext(Dispatchers.IO) {
                    cocktailRepository.getUserCheckmarksFromFirebase(userId)
                }

                val firebaseCheckmarks = firebaseResult.getOrElse {
                    Log.e(TAG, "getUserCheckmarksFromFirebase failed: ${it.message}", it)
                    emptyList()
                }

                val cocktailsFromFirebase: List<Cocktail> = withContext(Dispatchers.IO) {
                    firebaseCheckmarks.mapNotNull { cmk ->
                        runCatching { cocktailRepository.getCocktailById(cmk.cocktailId) }.getOrNull()
                    }
                }

                val localCheckmarks = withContext(Dispatchers.IO) {
                    cocktailRepository.getCheckmarksFromLocal(context)
                }

                if (cocktailsFromFirebase.isEmpty() && localCheckmarks.isNotEmpty()) {
                    Log.d(TAG, "Firebase empty & Local has ${localCheckmarks.size} → migrate up")
                    withContext(Dispatchers.IO) {
                        localCheckmarks.take(MAX_CHECKMARKS_ITEMS).forEach { c ->
                            runCatching { cocktailRepository.addCheckmarkToFirebase(userId, c.idDrink) }
                        }
                        cocktailRepository.saveCheckmarksToLocal(context, localCheckmarks)
                    }
                    cachedCheckmarks = localCheckmarks
                    view?.displayCheckmarks(localCheckmarks)
                } else {
                    val finalCheckmarks = deduplicateCheckmarks(cocktailsFromFirebase)
                    withContext(Dispatchers.IO) {
                        cocktailRepository.saveCheckmarksToLocal(context, finalCheckmarks)
                    }
                    cachedCheckmarks = finalCheckmarks
                    if (finalCheckmarks.isEmpty()) view?.displayEmptyState()
                    else view?.displayCheckmarks(finalCheckmarks)
                    Log.d(TAG, "Synced Firebase ${cocktailsFromFirebase.size} → final ${finalCheckmarks.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing checkmarks: ${e.message}", e)
                loadFromLocalOnly()
            } finally {
                view?.displayLoading(false)
                isLoading = false
            }
        }
    }

    private fun loadFromLocalOnly() {
        val localCheckmarks = cocktailRepository.getCheckmarksFromLocal(context)
        cachedCheckmarks = localCheckmarks
        view?.displayLoading(false)
        view?.displayCheckmarks(localCheckmarks)
        isLoading = false
    }

    private fun deduplicateCheckmarks(checkmarks: List<Cocktail>): List<Cocktail> {
        return checkmarks.distinctBy { it.idDrink }
    }

    override fun addToCheckmarks(cocktail: Cocktail) {
        if (isProcessingCheckmarks) return
        isProcessingCheckmarks = true

        val currentUser = auth.currentUser
        if (currentUser == null) {
            addToLocalCheckmarks(cocktail)
            isProcessingCheckmarks = false
            return
        }

        uiScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    cocktailRepository.addCheckmarkToFirebase(currentUser.uid, cocktail.idDrink)
                }

                if (result.isSuccess) {
                    addToLocalCheckmarks(cocktail)
                    view?.showCheckmarkAdded(cocktail)
                } else {
                    Log.e(TAG, "Failed to add checkmark to Firebase: ${result.exceptionOrNull()}")
                    addToLocalCheckmarks(cocktail)
                    view?.showCheckmarkAdded(cocktail)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding checkmark", e)
                addToLocalCheckmarks(cocktail)
                view?.showCheckmarkAdded(cocktail)
            } finally {
                isProcessingCheckmarks = false
            }
        }
    }

    private fun addToLocalCheckmarks(cocktail: Cocktail) {
        if (!cachedCheckmarks.any { it.idDrink == cocktail.idDrink }) {
            cachedCheckmarks = cachedCheckmarks + cocktail
            cocktailRepository.saveCheckmarksToLocal(context, cachedCheckmarks)
        }
    }

    override fun removeFromCheckmarks(cocktail: Cocktail) {
        if (isProcessingCheckmarks) return
        isProcessingCheckmarks = true

        uiScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    val localCheckmarks = withContext(Dispatchers.IO) {
                        cocktailRepository.getCheckmarksFromLocal(context).toMutableList().apply {
                            removeAll { it.idDrink == cocktail.idDrink }
                            cocktailRepository.saveCheckmarksToLocal(context, this)
                        }
                    }
                    cachedCheckmarks = localCheckmarks
                    if (localCheckmarks.isEmpty()) view?.displayEmptyState() else view?.displayCheckmarks(localCheckmarks)
                    view?.showCheckmarkRemoved(cocktail)
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    cocktailRepository.removeCheckmarkFromFirebase(currentUser.uid, cocktail.idDrink)
                }

                if (result.isSuccess) {
                    val localCheckmarks = withContext(Dispatchers.IO) {
                        cocktailRepository.getCheckmarksFromLocal(context).toMutableList().apply {
                            removeAll { it.idDrink == cocktail.idDrink }
                            cocktailRepository.saveCheckmarksToLocal(context, this)
                        }
                    }
                    cachedCheckmarks = localCheckmarks
                    if (localCheckmarks.isEmpty()) view?.displayEmptyState() else view?.displayCheckmarks(localCheckmarks)
                    view?.showCheckmarkRemoved(cocktail)
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to remove from checkmarks")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from checkmarks", e)
                view?.displayError("Error removing from checkmarks: ${e.message}")
            } finally {
                isProcessingCheckmarks = false
            }
        }
    }

    override fun toggleCheckmark(cocktail: Cocktail) {
        if (cachedCheckmarks.any { it.idDrink == cocktail.idDrink }) {
            removeFromCheckmarks(cocktail)
        } else {
            addToCheckmarks(cocktail)
        }
    }

    override fun syncCheckmarksIfNeeded() {
        val currentUser = auth.currentUser
        if (currentUser != null && isNetworkAvailable()) {
            view?.showSyncStatus("Syncing checkmarks...")
            syncFirebaseWithLocal(currentUser.uid)
        } else {
            view?.showSyncStatus("Offline mode - showing local checkmarks")
        }
    }

    override fun clearAllCheckmarks() {
        uiScope.launch {
            try {
                val currentUser = auth.currentUser
                withContext(Dispatchers.IO) { cocktailRepository.clearAllCheckmarksFromLocal(context) }

                if (currentUser != null && isNetworkAvailable()) {
                    val result = withContext(Dispatchers.IO) {
                        cocktailRepository.clearAllCheckmarksFromFirebase(currentUser.uid)
                    }
                    if (result.isSuccess) {
                        Log.d(TAG, "Cleared all checkmarks from Firebase and local")
                        view?.showSyncStatus("All checkmarks cleared")
                    } else {
                        Log.w(TAG, "Failed to clear Firebase checkmarks, but local cleared")
                        view?.showSyncStatus("Local checkmarks cleared")
                    }
                } else {
                    Log.d(TAG, "Cleared local checkmarks only (no user/offline)")
                    view?.showSyncStatus("Local checkmarks cleared")
                }
                cachedCheckmarks = emptyList()
                view?.displayEmptyState()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing checkmarks", e)
                view?.displayError("Error clearing checkmarks: ${e.message}")
            }
        }
    }
}
