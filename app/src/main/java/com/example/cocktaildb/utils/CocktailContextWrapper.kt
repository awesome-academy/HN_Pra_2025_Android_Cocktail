package com.example.cocktaildb.utils

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope

class CocktailContextWrapper(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : LifecycleOwner by lifecycleOwner {
    
    val coroutineScope: CoroutineScope
        get() = lifecycleScope
    fun getString(resId: Int): String = context.getString(resId)
    fun getString(resId: Int, vararg formatArgs: Any): String = context.getString(resId, *formatArgs)
}
