package com.example.cocktaildb.data.repository

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.source.CocktailDataSource
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource

class CocktailRepository(private val dataSource: CocktailDataSource = CocktailRemoteDataSource()) {

    fun getCocktails(): List<Cocktail> {
        return dataSource.getCocktails()
    }

    fun getCocktailById(id: String): Cocktail? {
        return dataSource.getCocktailById(id)
    }

    suspend fun fetchCocktailsFromApi(): List<Cocktail> {
        return dataSource.fetchCocktailsFromApi()
    }

    fun searchCocktails(query: String): List<Cocktail> {
        return dataSource.searchCocktails(query)
    }

    fun filterByCategory(category: String): List<Cocktail> {
        return dataSource.filterByCategory(category)
    }

    fun filterByAlcoholic(alcoholic: String): List<Cocktail> {
        return dataSource.filterByAlcoholic(alcoholic)
    }

    fun getCategories(): List<String> {
        return dataSource.getCategories()
    }
}

