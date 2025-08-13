package com.example.cocktaildb.data.repository.source

import com.example.cocktaildb.data.model.Cocktail

interface CocktailDataSource {
    fun getCocktails(): List<Cocktail>
    fun getCocktailById(id: String): Cocktail?
    suspend fun fetchCocktailsFromApi(): List<Cocktail>
}

