package com.example.cocktaildb.data.repository.source

import com.example.cocktaildb.data.model.Cocktail

interface CocktailDataSource {
    fun getCocktails(): List<Cocktail>
    fun getAllCocktails(): List<Cocktail>
    fun getCocktailById(id: String): Cocktail?
    suspend fun fetchCocktailsFromApi(): List<Cocktail>
    fun searchCocktails(query: String): List<Cocktail>
    fun filterByCategory(category: String): List<Cocktail>
    fun filterByAlcoholic(alcoholic: String): List<Cocktail>
    fun getCategories(): List<String>
    fun loadMoreCocktails(): List<Cocktail>
    fun getAlcoholicTypes(): List<String>
}


