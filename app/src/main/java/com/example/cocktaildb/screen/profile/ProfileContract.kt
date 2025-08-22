package com.example.cocktaildb.screen.profile

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter


interface ProfileContract {


    interface View {

        fun showUserProfile(userName: String, userBio: String, profileImageUrl: String?)


        fun showUserCocktails(cocktails: List<Cocktail>)


        fun navigateToMyRecipes()

        fun navigateToHistory()

        fun navigateToCheckmarks()

        fun navigateToLogin()


        fun displayLoading(show: Boolean)


        fun displayError(message: String)
    }


    interface Presenter : BasePresenter<View> {

        fun loadUserProfile()


        fun loadUserCocktails()


        fun onMyRecipesClicked()


        fun onCheckmarkClicked()


        fun onHistoryClicked()


        fun onLogoutClicked()
    }
}


