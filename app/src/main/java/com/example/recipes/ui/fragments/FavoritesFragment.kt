package com.example.recipes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.R
import com.example.recipes.databinding.FragmentFavoritesBinding
import com.example.recipes.ui.activities.LoginActivity
import com.example.recipes.ui.adapters.RecipeAdapter
import com.example.recipes.viewmodel.FavoritesViewModel
import com.google.firebase.auth.FirebaseAuth

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val favoritesViewModel: FavoritesViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        auth.currentUser?.uid?.let { favoritesViewModel.loadFavorites(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoritesBinding.bind(view)

        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        val adapter = RecipeAdapter(
            recipes = emptyList(),
            showDelete = true,
            onItemClick = { recipe ->
                // pass recipe.id instead of the whole object
                val action = FavoritesFragmentDirections
                    .actionFavoritesToRecipeDetail(recipe.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { recipe ->
                favoritesViewModel.removeFavorite(userId, recipe)
                Toast.makeText(requireContext(), "Removed \"${recipe.title}\"", Toast.LENGTH_SHORT).show()
            }
        )

        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val recipe = adapter.currentList[viewHolder.adapterPosition]
                favoritesViewModel.removeFavorite(userId, recipe)
                Toast.makeText(requireContext(), "Removed \"${recipe.title}\"", Toast.LENGTH_SHORT).show()
            }
        }).attachToRecyclerView(binding.favoritesRecyclerView)

        favoritesViewModel.favoritesList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
        favoritesViewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                favoritesViewModel.clearError()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
