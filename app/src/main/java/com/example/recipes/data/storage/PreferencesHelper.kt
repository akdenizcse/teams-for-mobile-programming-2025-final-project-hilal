package com.example.recipes.data.storage

import android.content.Context

class PreferencesHelper(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("diet_preferences", Context.MODE_PRIVATE)

    fun savePreferences(vegetarian: Boolean, vegan: Boolean, glutenFree: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("vegetarian", vegetarian)
        editor.putBoolean("vegan", vegan)
        editor.putBoolean("gluten_free", glutenFree)
        editor.apply()
    }

    fun loadPreferences(): Map<String, Boolean> {
        val preferences = mutableMapOf<String, Boolean>()
        preferences["vegetarian"] = sharedPreferences.getBoolean("vegetarian", false)
        preferences["vegan"] = sharedPreferences.getBoolean("vegan", false)
        preferences["gluten_free"] = sharedPreferences.getBoolean("gluten_free", false)
        return preferences
    }
}
