package com.example.recipes.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.recipes.R
import com.example.recipes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Use toolbar as ActionBar
        setSupportActionBar(binding.toolbar)

        // 2) Grab NavController
        val host = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = host.navController

        // 3) Top-level destinations (no Up arrow)
        val appBarConfig = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.searchFragment,
                R.id.favoritesFragment,
                R.id.shoppingListFragment
            )
        )

        // 4) Wire up toolbar & bottom nav
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)
        binding.bottomNav.setupWithNavController(navController)

        // 5) Ensure tapping Home icon always pops back to homeFragment
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.homeFragment) {
                navController.popBackStack(R.id.homeFragment, false)
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val host = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        return host.navController.navigateUp() || super.onSupportNavigateUp()
    }
}
