package com.example.cocktaildb.data.model
data class CocktailDetail(
    val idDrink: String = "",
    val strDrink: String = "",
    val strCategory: String = "",
    val strAlcoholic: String = "",
    val strGlass: String = "",
    val strInstructions: String = "",
    val strDrinkThumb: String = "",
    val ingredients: List<String> = emptyList(),
    val measures: List<String> = emptyList()
) {
    constructor() : this("", "", "", "", "", "", "", emptyList(), emptyList())
    fun getIngredientsWithMeasures(): List<Pair<String?, String?>> {
        return ingredients.mapIndexed { index, ingredient ->
            val measure = if (index < measures.size) measures[index] else null
            ingredient to measure
        }
    }
    fun toCocktail(): Cocktail {
        return Cocktail(
            idDrink = idDrink,
            strDrink = strDrink,
            strCategory = strCategory,
            strAlcoholic = strAlcoholic,
            strGlass = strGlass,
            strInstructions = strInstructions,
            strDrinkThumb = strDrinkThumb,
            ingredients = ingredients,
            measures = measures
        )
    }
    
    companion object {
        fun fromCocktail(cocktail: Cocktail): CocktailDetail {
            return CocktailDetail(
                idDrink = cocktail.idDrink,
                strDrink = cocktail.strDrink,
                strCategory = cocktail.strCategory ?: "",
                strAlcoholic = cocktail.strAlcoholic ?: "",
                strGlass = cocktail.strGlass ?: "",
                strInstructions = cocktail.strInstructions ?: "",
                strDrinkThumb = cocktail.strDrinkThumb ?: "",
                ingredients = cocktail.ingredients,
                measures = cocktail.measures
            )
        }
    }
}
