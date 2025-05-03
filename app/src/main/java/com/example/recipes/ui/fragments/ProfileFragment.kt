package com.example.recipes.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.recipes.R
import com.example.recipes.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Track whether we're in edit mode
    private var isEditing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Populate fields
        binding.emailValueTextView.text = user.email
        binding.nameEditText.setText(user.displayName)

        // Button toggles between Edit and Save
        binding.editSaveButton.setOnClickListener {
            if (!isEditing) {
                // → ENTER edit mode
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
                // → SAVE changes
                val newName = binding.nameEditText.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Build profile update request
                val profileUpdates = userProfileChangeRequest {
                    displayName = newName
                }

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), getString(R.string.update_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.update_failed), Toast.LENGTH_SHORT).show()
                        }
                        // Exit edit mode
                        isEditing = false
                        binding.nameEditText.apply {
                            isEnabled = false
                            isFocusable = false
                            isFocusableInTouchMode = false
                        }
                        binding.editSaveButton.text = getString(R.string.edit)
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}