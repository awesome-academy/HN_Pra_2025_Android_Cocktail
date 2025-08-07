package com.example.cocktaildb.data.repository.source.local

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.source.CocktailDataSource

class CocktailLocalDataSource : CocktailDataSource {

    override fun getCocktails(): List<Cocktail> {
        // TODO: Implement local data source
        return emptyList()
    }

    override fun getCocktailById(id: String): Cocktail? {
        // TODO: Implement local data source
        return null
    }
}
