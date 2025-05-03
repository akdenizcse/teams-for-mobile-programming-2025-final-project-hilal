package com.example.recipes.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.recipes.R
import com.example.recipes.data.model.Category
import com.example.recipes.databinding.FragmentHomeBinding
import com.example.recipes.ui.adapters.CategoryAdapter
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val categories = listOf(
        Category("Breakfast",   R.drawable.baseline_egg_24),
        Category("Lunch",       R.drawable.baseline_food_bank_24),
        Category("Dinner",      R.drawable.baseline_dinner_dining_24),
        Category("Dessert",     R.drawable.baseline_cake_24),
        Category("Vegan",       R.drawable.baseline_grass_24),
        Category("Vegetarian",  R.drawable.baseline_emoji_nature_24),
        Category("Gluten Free", R.drawable.baseline_grain_24),
        Category("Snack",       R.drawable.baseline_coffee_24)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentHomeBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // hide the default ActionBar on this screen
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // personalized greeting
        val email = auth.currentUser?.email
        val name = email
            ?.substringBefore("@")
            ?.replaceFirstChar { it.uppercase() }
            ?: "there"
        binding.greetingTextView.text = "Hi $name"

        // hook up RecyclerView
        binding.homeCategoriesRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = CategoryAdapter(categories) { category ->
                // navigate to the category’s recipe list
                val action = HomeFragmentDirections
                    .actionHomeToCategories(category.name)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // restore the ActionBar
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }
}
