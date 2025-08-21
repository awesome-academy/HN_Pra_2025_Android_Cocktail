package com.example.cocktaildb.utils

import android.content.Context

/**
 * Provides access to string resources from contexts where Context is not directly available
 */
object StringProvider {
    private var appContext: Context? = null
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    fun getString(resId: Int): String {
        return appContext?.getString(resId) ?: "String not available"
    }
    
    fun getString(resId: Int, vararg formatArgs: Any): String {
        return appContext?.getString(resId, *formatArgs) ?: "String not available"
    }
}
