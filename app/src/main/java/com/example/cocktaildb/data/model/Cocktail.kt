package com.example.cocktaildb.data.model

data class Cocktail(
    val idDrink: String,
    val strDrink: String,
    val strDrinkAlternate: String? = null,
    val strTags: String? = null,
    val strVideo: String? = null,
    val strCategory: String? = null,
    val strIBA: String? = null,
    val strAlcoholic: String? = null,
    val strGlass: String? = null,
    val strInstructions: String? = null,
    val strInstructionsES: String? = null,
    val strInstructionsDE: String? = null,
    val strInstructionsFR: String? = null,
    val strInstructionsIT: String? = null,
    val strInstructionsZH_HANS: String? = null,
    val strInstructionsZH_HANT: String? = null,
    val strDrinkThumb: String? = null,
    val ingredients: List<String> = emptyList(),
    val measures: List<String> = emptyList(),
    val strImageSource: String? = null,
    val strImageAttribution: String? = null,
    val strCreativeCommonsConfirmed: String? = null,
    val dateModified: String? = null,
    val rating: Float? = null
) {
    // Helper function to get ingredients with measures
    fun getIngredientsWithMeasures(): List<Pair<String?, String?>> {
        return ingredients.mapIndexed { index, ingredient ->
            val measure = if (index < measures.size) measures[index] else null
            ingredient to measure
        }
    }

    // Helper function to get all instructions
    fun getAllInstructions(): Map<String, String> {
        return mapOf(
            "EN" to strInstructions,
            "ES" to strInstructionsES,
            "DE" to strInstructionsDE,
            "FR" to strInstructionsFR,
            "IT" to strInstructionsIT,
            "ZH_HANS" to strInstructionsZH_HANS,
            "ZH_HANT" to strInstructionsZH_HANT
        ).filterValues { it != null }
            .mapValues { it.value!! }
    }

    // Helper properties for backward compatibility
    val id: String get() = idDrink
    val name: String get() = strDrink
    val description: String? get() = strInstructions
    val imageUrl: String? get() = strDrinkThumb
    val category: String? get() = strCategory
    val alcoholic: String? get() = strAlcoholic
}

