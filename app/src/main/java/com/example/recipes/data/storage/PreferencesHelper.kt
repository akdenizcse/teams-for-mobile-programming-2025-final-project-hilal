package com.example.recipes.data.storage

import android.content.Context

class PreferencesHelper(context: Context) {

    private val prefs = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME      = "diet_preferences"
        private const val KEY_VEGETARIAN  = "vegetarian"
        private const val KEY_VEGAN       = "vegan"
        private const val KEY_GLUTEN_FREE = "gluten_free"
        private const val KEY_NONE        = "none"
    }

    /**
     * Save the four dietary options in one go.
     * If `none` is true, all other flags are automatically cleared.
     */
    fun savePreferences(
        vegetarian: Boolean,
        vegan: Boolean,
        glutenFree: Boolean,
        none: Boolean
    ) {
        prefs.edit().apply {
            putBoolean(KEY_NONE, none)
            if (none) {
                // override all other flags
                putBoolean(KEY_VEGETARIAN, false)
                putBoolean(KEY_VEGAN,      false)
                putBoolean(KEY_GLUTEN_FREE,false)
            } else {
                putBoolean(KEY_VEGETARIAN, vegetarian)
                putBoolean(KEY_VEGAN,      vegan)
                putBoolean(KEY_GLUTEN_FREE,glutenFree)
            }
        }.apply()
    }

    /**
     * Load all four flags as a Map<String,Boolean>.
     */
    fun loadPreferences(): Map<String, Boolean> = mapOf(
        KEY_VEGETARIAN  to prefs.getBoolean(KEY_VEGETARIAN,  false),
        KEY_VEGAN       to prefs.getBoolean(KEY_VEGAN,       false),
        KEY_GLUTEN_FREE to prefs.getBoolean(KEY_GLUTEN_FREE, false),
        KEY_NONE        to prefs.getBoolean(KEY_NONE,        false)
    )

    /**
     * Build the Spoonacular `diet` query:
     * - Returns `null` if none is selected, or if no specific diets are chosen.
     * - Otherwise joins the active diets into "vegan", "vegetarian,gluten free", etc.
     */
    fun getDietQuery(): String? {
        if (prefs.getBoolean(KEY_NONE, false)) return null

        val list = mutableListOf<String>()
        if (prefs.getBoolean(KEY_VEGETARIAN, false))  list += "vegetarian"
        if (prefs.getBoolean(KEY_VEGAN, false))       list += "vegan"
        if (prefs.getBoolean(KEY_GLUTEN_FREE, false)) list += "gluten free"

        return list.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = ",")
    }

    /** Individual flag accessors */
    fun isVegetarian(): Boolean  = prefs.getBoolean(KEY_VEGETARIAN,  false)
    fun isVegan(): Boolean       = prefs.getBoolean(KEY_VEGAN,       false)
    fun isGlutenFree(): Boolean  = prefs.getBoolean(KEY_GLUTEN_FREE, false)
    fun isNone(): Boolean        = prefs.getBoolean(KEY_NONE,        false)

    /** Clears all dietary preferences */
    fun clearPreferences() {
        prefs.edit().clear().apply()
    }
}
