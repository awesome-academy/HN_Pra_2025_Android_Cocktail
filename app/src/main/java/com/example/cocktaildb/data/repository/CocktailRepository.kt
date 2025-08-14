package com.example.cocktaildb.data.repository

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.DataCocktail
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
    fun getCocktailSearch(): List<DataCocktail> {
        return dataSource.getCocktailSearch()
    }

    fun searchCocktails(query: String): List<DataCocktail> {
        return if (dataSource is CocktailRemoteDataSource) {
            dataSource.searchCocktails(query)
        } else {
            emptyList()
        }
    }
    fun filterByCategory(category: String): List<DataCocktail> {
        return if (dataSource is CocktailRemoteDataSource) {
            dataSource.filterByCategory(category)
        } else {
            emptyList()
        }
    }

    fun filterByAlcoholic(alcoholic: String): List<DataCocktail> {
        return if (dataSource is CocktailRemoteDataSource) {
            dataSource.filterByAlcoholic(alcoholic)
        } else {
            emptyList()
        }
    }

    fun getCategories(): List<String> {
        return if (dataSource is CocktailRemoteDataSource) {
            dataSource.getCategories()
        } else {
            emptyList()
        }
    }
}

