package com.example.cocktaildb.data.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.screen.favorites.FavoritesPresenter
import com.example.cocktaildb.screen.history.HistoryPresenter
import com.example.cocktaildb.screen.myrecipe.MyRecipePresenter
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.example.cocktaildb.utils.CocktailContextWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DataManager {
    private const val TAG = "DataManager"
    private var isDataLoading = false
    
    // Method to set services for testing
    private var recipeFirebaseService: RecipeFirebaseService = RecipeFirebaseService()
    
    fun setRecipeFirebaseService(service: RecipeFirebaseService) {
        recipeFirebaseService = service
    }

    fun autoLoadDataAfterLogin(context: Context, lifecycleOwner: LifecycleOwner) {
        if (isDataLoading) {
            Log.d(TAG, context.getString(R.string.msg_data_loading_in_progress))
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                isDataLoading = true
                
                val authRepository = AuthRepository(context)
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser == null) {
                    Log.d(TAG, context.getString(R.string.msg_no_user_logged_in))
                    return@launch
                }
                
                if (!isNetworkAvailable(context)) {
                    Log.d(TAG, context.getString(R.string.msg_no_network_available))
                    return@launch
                }
                
                Log.d(TAG, context.getString(R.string.msg_auto_loading_data_for_user, currentUser.uid))

                val cocktailRepository = CocktailRepository(CocktailRemoteDataSource())
                val contextWrapper = CocktailContextWrapper(context, lifecycleOwner)

                val favoritesJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "Loading favorites data in background...")
                        if (!FavoritesManager.isInitialized()) {
                            FavoritesManager.loadFavoritesFromFirestore { success ->
                                Log.d(TAG, if (success) "Favorites loaded via FavoritesManager" else "Failed to load favorites via FavoritesManager")
                            }
                        }
                        val favoritesPresenter = FavoritesPresenter(context, cocktailRepository)
                        favoritesPresenter.loadFavorites()
                        Log.d(TAG, "Favorites data loaded successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load favorites data", e)
                    }
                }
                
                val historyJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "Loading history data in background...")
                        val historyPresenter = HistoryPresenter(cocktailRepository, contextWrapper)
                        historyPresenter.loadHistoryCocktails()
                        Log.d(TAG, "History data loaded successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load history data", e)
                    }
                }
                
                val myRecipeJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "Loading my recipes data in background...")
                        val myRecipePresenter = MyRecipePresenter(recipeFirebaseService, authRepository)
                        myRecipePresenter.loadUserRecipes()
                        Log.d(TAG, "My recipes data loaded successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load my recipes data", e)
                    }
                }

                favoritesJob.join()
                historyJob.join()
                myRecipeJob.join()
                
                Log.d(TAG, "All data loading operations completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in autoLoadDataAfterLogin", e)
            } finally {
                isDataLoading = false
            }
        }
    }
    
    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                as? android.net.ConnectivityManager
            val networkInfo = connectivityManager?.activeNetworkInfo
            networkInfo?.isConnected == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            false
        }
    }
}
