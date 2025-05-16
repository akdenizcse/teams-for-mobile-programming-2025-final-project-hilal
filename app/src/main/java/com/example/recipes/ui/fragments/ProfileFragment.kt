package com.example.recipes.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.recipes.R
import com.example.recipes.data.repository.RecipeRepository
import com.example.recipes.data.storage.PreferencesHelper
import com.example.recipes.databinding.FragmentProfileBinding
import com.example.recipes.ui.activities.LoginActivity
import com.example.recipes.viewmodel.ProfileViewModel
import com.example.recipes.viewmodel.ViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PreferencesHelper
    private lateinit var vm:   ProfileViewModel
    private val auth get() = FirebaseAuth.getInstance()

    private var isEditing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // --- ViewModel + prefs ---
        prefs = PreferencesHelper(requireContext())
        vm    = ViewModelProvider(
            this,
            ViewModelFactory(prefs, RecipeRepository())
        )[ProfileViewModel::class.java]

        // --- User check ---
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Populate user fields ---
        binding.emailValueTextView.text = user.email
        binding.nameEditText.setText(user.displayName)

        // --- Initialise diet check-boxes from saved prefs ---
        binding.noneCheckBox.isChecked        = prefs.isNone()
        binding.vegetarianCheckBox.isChecked  = prefs.isVegetarian()
        binding.veganCheckBox.isChecked       = prefs.isVegan()
        binding.glutenFreeCheckBox.isChecked  = prefs.isGlutenFree()

        // “None” clears others
        binding.noneCheckBox.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.vegetarianCheckBox.isChecked = false
                binding.veganCheckBox.isChecked      = false
                binding.glutenFreeCheckBox.isChecked = false
            }
        }
        // Any diet flag unchecks “None”
        val dietListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.noneCheckBox.isChecked = false
        }
        binding.vegetarianCheckBox.setOnCheckedChangeListener(dietListener)
        binding.veganCheckBox.setOnCheckedChangeListener(dietListener)
        binding.glutenFreeCheckBox.setOnCheckedChangeListener(dietListener)

        // --- Edit / Save display-name ---
        binding.editSaveButton.setOnClickListener {
            if (!isEditing) enterEditMode() else saveProfileChanges(user)
        }

        // --- Save dietary prefs button ---
        binding.saveDietaryPreferencesButton.setOnClickListener {
            saveDietPreferences()
        }

        // --- Log out ---
        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            startActivity(
                Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }

    /** Switch UI to edit mode for display-name. */
    private fun enterEditMode() {
        isEditing = true
        binding.nameEditText.apply {
            isEnabled = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        binding.editSaveButton.text = getString(R.string.save)
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(binding.nameEditText, 0)
    }

    /** Persist new display-name and exit edit mode. */
    private fun saveProfileChanges(user: FirebaseUser) {
        val newName = binding.nameEditText.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        user.updateProfile(userProfileChangeRequest { displayName = newName })
            .addOnCompleteListener {
                Toast.makeText(
                    context,
                    if (it.isSuccessful) R.string.update_success else R.string.update_failed,
                    Toast.LENGTH_SHORT
                ).show()
                exitEditMode()
            }
    }

    /** Persist dietary flags via the ViewModel. */
    private fun saveDietPreferences() {
        vm.savePreferences(
            vegetarian = binding.vegetarianCheckBox.isChecked,
            vegan      = binding.veganCheckBox.isChecked,
            glutenFree = binding.glutenFreeCheckBox.isChecked,
            none       = binding.noneCheckBox.isChecked
        )
        Toast.makeText(requireContext(), R.string.update_success, Toast.LENGTH_SHORT).show()
    }

    private fun exitEditMode() {
        isEditing = false
        binding.nameEditText.apply {
            isEnabled = false
            clearFocus()
        }
        binding.editSaveButton.text = getString(R.string.edit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
