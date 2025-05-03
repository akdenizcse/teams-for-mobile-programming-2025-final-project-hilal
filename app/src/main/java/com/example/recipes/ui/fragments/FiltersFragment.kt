package com.example.recipes.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.recipes.databinding.FragmentFiltersBinding
import com.example.recipes.viewmodel.SearchViewModel



class FiltersFragment : Fragment() {

    private var _binding: FragmentFiltersBinding? = null
    private val binding get() = _binding!!

    // Use the same SearchViewModel that SearchFragment uses
    private val searchViewModel: SearchViewModel by activityViewModels()

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

        binding.applyFiltersButton.setOnClickListener {
            // 1) Gather which checkboxes are checked
            val selected = mutableListOf<String>()
            if (binding.vegetarianCheckBox.isChecked)   selected += "vegetarian"
            if (binding.veganCheckBox.isChecked)        selected += "vegan"
            if (binding.glutenFreeCheckBox.isChecked)   selected += "gluten free"

            // 2) Create a comma-separated diet filter
            val dietParam = selected.joinToString(",")

            // 3) Tell ViewModel to apply filters and re-run the last search

            searchViewModel.applyFilters(dietParam)

            // 4) Go back to SearchFragment (results will auto-update)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
