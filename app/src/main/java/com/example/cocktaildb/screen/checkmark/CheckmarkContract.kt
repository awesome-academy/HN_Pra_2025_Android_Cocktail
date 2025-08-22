package com.example.cocktaildb.screen.checkmark

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface CheckmarkContract {

    interface View {
        fun displayLoading(show: Boolean)
        fun displayCheckmarks(checkmarks: List<Cocktail>)
        fun displayEmptyState()
        fun displayError(message: String)
        fun showCheckmarkAdded(cocktail: Cocktail)
        fun showCheckmarkRemoved(cocktail: Cocktail)
        fun showSyncStatus(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadCheckmarks()
        fun addToCheckmarks(cocktail: Cocktail)
        fun removeFromCheckmarks(cocktail: Cocktail)
        fun toggleCheckmark(cocktail: Cocktail)
        fun syncCheckmarksIfNeeded()
        fun clearAllCheckmarks()
    }
} 