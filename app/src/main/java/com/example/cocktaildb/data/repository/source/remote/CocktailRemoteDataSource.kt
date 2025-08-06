package com.example.cocktaildb.data.repository.source.remote

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.source.CocktailDataSource

class CocktailRemoteDataSource : CocktailDataSource {
    
    override fun getCocktails(): List<Cocktail> {
        // TODO: Implement remote data source
        return emptyList()
    }
    
    override fun getCocktailById(id: String): Cocktail? {
        // TODO: Implement remote data source
        return null
    }
} 