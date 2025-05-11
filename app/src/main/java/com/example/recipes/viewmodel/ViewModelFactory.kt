package com.example.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recipes.data.repository.RecipeRepository
import com.example.recipes.data.storage.PreferencesHelper

class ViewModelFactory(
    private val preferencesHelper: PreferencesHelper,
    private val recipeRepository: RecipeRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(preferencesHelper, recipeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
