package com.example.recipes.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.recipes.R
import com.example.recipes.data.storage.PreferencesHelper
import com.example.recipes.databinding.FragmentProfileBinding
import com.example.recipes.viewmodel.ProfileViewModel
import com.example.recipes.viewmodel.ViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.example.recipes.data.repository.RecipeRepository

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var profileViewModel: ProfileViewModel

    private var isEditing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // Initialize preferences helper and view model
        preferencesHelper = PreferencesHelper(requireContext())
        val recipeRepository = RecipeRepository()
        val viewModelFactory = ViewModelFactory(preferencesHelper, recipeRepository)
        profileViewModel = ViewModelProvider(this, viewModelFactory).get(ProfileViewModel::class.java)

        // Check if the user is logged in
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Populate the email and name fields
        binding.emailValueTextView.text = user.email
        binding.nameEditText.setText(user.displayName)

        // Load existing preferences and update UI accordingly
        profileViewModel.loadPreferences()
        profileViewModel.filteredRecipes.observe(viewLifecycleOwner) { recipes ->
            Log.d("ProfileFragment", "Filtered Recipes: $recipes")  // Log filtered recipes
            if (recipes.isNotEmpty()) {
                // Example: binding.recyclerView.adapter = RecipeAdapter(recipes)
                // Update the RecyclerView with the filtered recipes (depending on your RecyclerView adapter)
            } else {
                Toast.makeText(requireContext(), "No recipes found for your preferences", Toast.LENGTH_SHORT).show()
            }
        }

        // Toggle edit/save button behavior
        binding.editSaveButton.setOnClickListener {
            if (!isEditing) {
                // Enter edit mode
                isEditing = true
                binding.nameEditText.apply {
                    isEnabled = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                }
                binding.editSaveButton.text = getString(R.string.save)

                // Show the soft keyboard
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.nameEditText, InputMethodManager.SHOW_IMPLICIT)
            } else {
                // Save changes
                val newName = binding.nameEditText.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val profileUpdates = userProfileChangeRequest {
                    displayName = newName
                }

                // Save dietary preferences
                val vegetarian = binding.vegetarianCheckBox.isChecked
                val vegan = binding.veganCheckBox.isChecked
                val glutenFree = binding.glutenFreeCheckBox.isChecked
                profileViewModel.savePreferences(vegetarian, vegan, glutenFree)

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), getString(R.string.update_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.update_failed), Toast.LENGTH_SHORT).show()
                        }

                        // Exit edit mode and reset the button text
                        isEditing = false
                        binding.nameEditText.apply {
                            isEnabled = false
                            isFocusable = false
                            isFocusableInTouchMode = false
                        }
                        binding.editSaveButton.text = getString(R.string.edit)

                        // Trigger the recipe filtering after saving preferences
                        filterRecipesBasedOnPreferences()
                    }
            }
        }

        // Listen for changes in preferences and filter the recipes accordingly
        binding.vegetarianCheckBox.setOnCheckedChangeListener { _, _ -> updatePreferencesAndFilter() }
        binding.veganCheckBox.setOnCheckedChangeListener { _, _ -> updatePreferencesAndFilter() }
        binding.glutenFreeCheckBox.setOnCheckedChangeListener { _, _ -> updatePreferencesAndFilter() }
    }

    // Update preferences and filter recipes
    private fun updatePreferencesAndFilter() {
        val vegetarian = binding.vegetarianCheckBox.isChecked
        val vegan = binding.veganCheckBox.isChecked
        val glutenFree = binding.glutenFreeCheckBox.isChecked

        // Log the preferences to verify that they are set correctly
        Log.d("ProfileFragment", "Updated Preferences: Vegetarian: $vegetarian, Vegan: $vegan, Gluten-Free: $glutenFree")

        // Save the updated preferences and trigger the search
        profileViewModel.savePreferences(vegetarian, vegan, glutenFree)
        profileViewModel.searchRecipes(vegetarian, vegan, glutenFree)
    }

    // Trigger the recipe filtering after saving preferences
    private fun filterRecipesBasedOnPreferences() {
        val vegetarian = binding.vegetarianCheckBox.isChecked
        val vegan = binding.veganCheckBox.isChecked
        val glutenFree = binding.glutenFreeCheckBox.isChecked

        profileViewModel.searchRecipes(vegetarian, vegan, glutenFree)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
