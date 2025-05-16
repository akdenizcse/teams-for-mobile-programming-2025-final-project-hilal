package com.example.recipes.ui.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.recipes.data.repository.RecipeRepository
import com.example.recipes.data.storage.PreferencesHelper
import com.example.recipes.databinding.FragmentFiltersBinding
import com.example.recipes.viewmodel.SearchViewModel

class FiltersFragment : Fragment() {
    private var _binding: FragmentFiltersBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PreferencesHelper

    // Obtain the shared SearchViewModel, injecting prefs
    private val searchViewModel: SearchViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(
                    repo = RecipeRepository(),
                    prefs = prefs
                ) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferencesHelper(requireContext())

        // 1) Initialize checkboxes from saved prefs
        binding.noneCheckBox.isChecked        = prefs.isNone()
        binding.vegetarianCheckBox.isChecked  = prefs.isVegetarian()
        binding.veganCheckBox.isChecked       = prefs.isVegan()
        binding.glutenFreeCheckBox.isChecked  = prefs.isGlutenFree()

        // 2) Mutual-exclusion: checking None clears the others
        binding.noneCheckBox.setOnCheckedChangeListener { _, noneChecked ->
            if (noneChecked) {
                binding.vegetarianCheckBox.isChecked = false
                binding.veganCheckBox.isChecked      = false
                binding.glutenFreeCheckBox.isChecked = false
            }
        }
        // Checking any diet option unchecks None
        val dietListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.noneCheckBox.isChecked = false
        }
        binding.vegetarianCheckBox.setOnCheckedChangeListener(dietListener)
        binding.veganCheckBox.setOnCheckedChangeListener(dietListener)
        binding.glutenFreeCheckBox.setOnCheckedChangeListener(dietListener)

        // 3) Save & apply filters
        binding.applyFiltersButton.setOnClickListener {
            prefs.savePreferences(
                vegetarian = binding.vegetarianCheckBox.isChecked,
                vegan      = binding.veganCheckBox.isChecked,
                glutenFree = binding.glutenFreeCheckBox.isChecked,
                none       = binding.noneCheckBox.isChecked
            )
            searchViewModel.refreshLastSearch()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
