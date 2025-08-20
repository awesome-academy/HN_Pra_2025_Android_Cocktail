package com.example.cocktaildb.screen.detail

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface CocktailDetailContract {
    interface View {
        fun showRelatedCocktails(cocktails: List<Cocktail>)
        fun showError(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadRelatedCocktails(cocktailName: String, category: String)
    }
}

package com.example.cocktaildb.screen.detail

import com.example.cocktaildb.data.model.Cocktail


interface CocktailDetailContract {

    interface View {

        fun updateBookmarkButtonState(isBookmarked: Boolean)


        fun showError(message: String)
    }


    interface Presenter {

        fun setView(view: View?)


        fun checkBookmarkStatus(cocktailId: String)


        fun toggleBookmark(cocktail: Cocktail)


        fun onStop()
    }
}

