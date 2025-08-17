package com.example.cocktaildb.data.repository.source.local

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.source.CocktailDataSource

class CocktailLocalDataSource : CocktailDataSource {

    override fun getCocktails(): List<Cocktail> {
        // TODO: Implement local data source with Room database
        return emptyList()
    }

    override fun getCocktailById(id: String): Cocktail? {
        // TODO: Implement local data source with Room database
        return null
    }

    override suspend fun fetchCocktailsFromApi(): List<Cocktail> {
        // Local data source doesn't fetch from API
        return emptyList()
    }

    override fun searchCocktails(query: String): List<Cocktail> {
        // TODO: Implement local search
        return emptyList()
    }

    override fun filterByCategory(category: String): List<Cocktail> {
        // TODO: Implement local filter
        return emptyList()
    }

    override fun filterByAlcoholic(alcoholic: String): List<Cocktail> {
        // TODO: Implement local filter
        return emptyList()
    }

    override fun getCategories(): List<String> {
        // TODO: Implement local categories
        return emptyList()
    }
}

