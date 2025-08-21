package com.example.cocktaildb.screen.home

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.utils.base.BaseFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

class HomePresenter(
    private val cocktailRepository: CocktailRepository,
    private val authRepository: AuthRepository,
    private val historyFirebaseService: HistoryFirebaseService
) : HomeContract.Presenter {

    private var view: HomeContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())

    override fun setView(view: HomeContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // No initialization needed
    }

    override fun onStop() {
        presenterScope.cancel() // Cancel all coroutines when stopping
        view = null
    }

    override fun loadCocktails() {
        (view as? BaseFragment<*>)?.showLoading()
        
        presenterScope.launch {
            try {
                val cocktails = withContext(Dispatchers.IO) {
                    cocktailRepository.fetchCocktailsFromApi()
                }
                view?.showCocktails(cocktails)
            } catch (e: Exception) {
                (view as? BaseFragment<*>)?.showError(e.message ?: "Unknown error")
            } finally {
                (view as? BaseFragment<*>)?.hideLoading()
            }
        }
    }

    override fun onCocktailClicked(cocktail: Cocktail) {
        addToHistory(cocktail)
        view?.navigateToCocktailDetail(cocktail)
    }

    override fun addToHistory(cocktail: Cocktail) {
        Log.e("HomePresenter", "addToHistory called for: ${cocktail.strDrink} (${cocktail.idDrink})")

        val currentUser = authRepository.getCurrentUser()

        if (currentUser != null) {
            Log.e("HomePresenter", "User authenticated: ${currentUser.uid}")

            presenterScope.launch {
                try {
                    Log.e("HomePresenter", "Adding cocktail details to Firebase history: uid=${currentUser.uid}, cocktail=${cocktail.strDrink}")
                    val result = historyFirebaseService.addHistoryWithDetails(currentUser.uid, cocktail)
                    if (result.isSuccess) {
                        Log.e("HomePresenter", "Successfully added detailed history: ${result.getOrNull()}")
                    } else {
                        Log.e("HomePresenter", "Failed to add detailed history: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("HomePresenter", "Exception adding detailed history: ${e.message}", e)
                }
            }
        } else {
            Log.e("HomePresenter", "User not authenticated, skipping history add")
        }
    }
}
