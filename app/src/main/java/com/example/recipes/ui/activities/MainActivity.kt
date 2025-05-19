// app/src/main/java/com/example/recipes/ui/activities/MainActivity.kt
package com.example.recipes.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
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

        /* NavController from the <fragment> in activity_main.xml */
        val host = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = host.navController

        /* “top-level” tabs (Bottom-Nav) */
        val topLevel = setOf(
            R.id.homeFragment,
            R.id.favoritesFragment,
            R.id.shoppingListFragment,
            R.id.profileFragment,
            R.id.ordersFragment          // your Orders tab
        )
        val appBarConfig = AppBarConfiguration(topLevel)

        /* Hook up the bottom-nav (there is no ActionBar anymore) */
        binding.bottomNav.setupWithNavController(navController)

        /* Always pop to Home root when Home tab tapped */
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.homeFragment) {
                navController.popBackStack(R.id.homeFragment, false)
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
    }

    /* Up-navigation falls back to NavController */
    override fun onSupportNavigateUp(): Boolean {
        val host = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        return host.navController.navigateUp() || super.onSupportNavigateUp()
    }

    // MainActivity.kt
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_cart -> {
                findNavController(R.id.navHostFragment)
                    .navigate(R.id.shoppingListFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
