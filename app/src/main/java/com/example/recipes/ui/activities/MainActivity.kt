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

        // 1) Toolbar as ActionBar
        setSupportActionBar(binding.toolbar)

        // 2) Grab NavController from the FragmentContainerView
        val host = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = host.navController

        // 3) Which destinations are top-level (no Up arrow)
        val topLevel = setOf(
            R.id.homeFragment,
            R.id.favoritesFragment,
            R.id.shoppingListFragment,
            R.id.profileFragment
        )
        val appBarConfig = AppBarConfiguration(topLevel)

        // 4) Hook up ActionBar & BottomNav with NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)
        binding.bottomNav.setupWithNavController(navController)

        // 5) Ensure Home tap always pops to root
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
