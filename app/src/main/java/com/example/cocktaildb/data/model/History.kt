package com.example.cocktaildb.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
data class History(
    val id: String = "",
    val uid: String = "",
    val cocktailId: String = "",
    val cocktailDetail: CocktailDetail? = null,
    val viewedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", null, System.currentTimeMillis())
}
