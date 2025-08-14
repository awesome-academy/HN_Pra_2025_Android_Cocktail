package com.example.cocktaildb.data.repository.source

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.DataCocktail

interface CocktailDataSource {
    fun getCocktails(): List<Cocktail>
    fun getCocktailById(id: String): Cocktail?
    suspend fun fetchCocktailsFromApi(): List<Cocktail>
    fun getCocktailSearch(): List<DataCocktail>
    fun getCocktailByIdSearch(id: String): DataCocktail?
}

