package com.example.cocktaildb.screen.profile

import android.os.AsyncTask
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Presenter implementation for the Profile screen
 */
class ProfilePresenter(
    private val cocktailRepository: CocktailRepository,
    private val authRepository: AuthRepository = AuthRepository()
) : ProfileContract.Presenter {

    private var view: ProfileContract.View? = null

    override fun setView(view: ProfileContract.View?) {
        this.view = view
        if (view != null) {
            loadData()
        }
    }

    override fun onStart() {
        // This method is called when the view starts
        // If needed, refresh data here
    }

    override fun onStop() {
        // Clean up resources when the view stops
        this.view = null
    }

    override fun loadUserProfile() {
        view?.displayLoading(true)

        // Get the current logged-in user
        val currentUser = authRepository.getCurrentUser()

        if (currentUser != null) {
            try {
                // Get uid of the current user
                val uid = currentUser.uid

                // Fetch user profile data from Firestore
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        view?.displayLoading(false)
                        if (document != null && document.exists()) {
                            // Change user data to user object
                            val userData = document.data
                            if (userData != null) {
                                val userName = userData["name"] as? String ?: "User"
                                val userEmail = userData["email"] as? String ?: ""
                                val profileImage = userData["profileImage"] as? String

                                view?.showUserProfile(
                                    userName = userName,
                                    userBio = userEmail,
                                    profileImageUrl = profileImage
                                )
                            } else {
                                // if user data is null, show default profile
                                view?.showUserProfile(
                                    userName = "User",
                                    userBio = "No profile information",
                                    profileImageUrl = null
                                )
                            }
                        } else {
                            // if document does not exist, show default profile
                            view?.showUserProfile(
                                userName = "User",
                                userBio = "No profile information",
                                profileImageUrl = null
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        view?.displayLoading(false)
                        view?.displayError("Error loading profile: ${e.message}")
                        // Display default profile information on error
                        view?.showUserProfile(
                            userName = "User",
                            userBio = "Could not load profile",
                            profileImageUrl = null
                        )
                    }
            } catch (e: Exception) {
                view?.displayLoading(false)
                view?.displayError("Error accessing user data: ${e.message}")
                // Display default profile information on exception
                view?.showUserProfile(
                    userName = "User",
                    userBio = "Error loading profile",
                    profileImageUrl = null
                )
            }
        } else {
            // No user is logged in
            view?.displayLoading(false)
            view?.showUserProfile(
                userName = "Guest User",
                userBio = "Please sign in to see your profile",
                profileImageUrl = null
            )
        }
    }

    override fun loadUserCocktails() {
        view?.displayLoading(true)

        // Using AsyncTask instead of Coroutines
        @Suppress("DEPRECATION")
        CocktailLoadTask().execute()
    }

    override fun onMyRecipesClicked() {
        // Navigate to My Recipes screen
        view?.navigateToMyRecipes()
    }

    override fun onHistoryClicked() {
        // Handle History button click
        // This would typically navigate to a history screen
    }

    override fun onLogoutClicked() {
        // Call the AuthRepository's signOut method to log the user out
        authRepository.signOut()

        // Navigate to login screen
        view?.navigateToLogin()
    }

    private fun loadData() {
        loadUserProfile()
        loadUserCocktails()
    }

    /**
     * AsyncTask to load cocktails in background
     */
    @Suppress("DEPRECATION")
    private inner class CocktailLoadTask : AsyncTask<Void, Void, List<Cocktail>>() {

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): List<Cocktail> {
            // Get cocktails from repository (currently returns empty list)
            val repoList = cocktailRepository.getCocktails()

            // Since the repository returns empty list, use mock data instead
            return if (repoList.isEmpty()) createMockCocktails() else repoList
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: List<Cocktail>) {
            view?.displayLoading(false)
            view?.showUserCocktails(result)
        }

        @Deprecated("Deprecated in Java")
        override fun onCancelled() {
            view?.displayLoading(false)
            view?.displayError("Failed to load cocktails")
        }
    }

    /**
     * Creates mock cocktail data for display when repository is empty
     */
    private fun createMockCocktails(): List<Cocktail> {
        return listOf(
            Cocktail(
                id = "1",
                name = "Mojito",
                description = "Refreshing rum cocktail with mint and lime",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/metwgh1606770327.jpg"
            ),
            Cocktail(
                id = "2",
                name = "Margarita",
                description = "Classic tequila cocktail with lime",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg"
            ),
            Cocktail(
                id = "3",
                name = "Piña Colada",
                description = "Tropical rum cocktail with coconut and pineapple",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/cpf4j51504371346.jpg"
            ),
            Cocktail(
                id = "4",
                name = "Virgin Mojito",
                description = "Non-alcoholic version of the classic Mojito",
                imageUrl = "https://www.thecocktaildb.com/images/media/drink/vr6kle1504886114.jpg"
            )
        )
    }
}
