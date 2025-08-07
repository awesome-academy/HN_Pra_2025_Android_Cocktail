package com.example.cocktaildb.data.repository

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.source.CocktailDataSource

class CocktailRepository(private val dataSource: CocktailDataSource) {

    fun getCocktails(): List<Cocktail> {
        return dataSource.getCocktails()
    }

    fun getCocktailById(id: String): Cocktail? {
        return dataSource.getCocktailById(id)
    }
}

