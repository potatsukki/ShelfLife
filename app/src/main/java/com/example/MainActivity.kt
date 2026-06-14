package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ShelfLifeBottomBar
import com.example.ui.components.ShelfLifeTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ShelfLifeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ShelfLifeViewModel = viewModel()
            val isDarkModeEnabled by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkModeEnabled) {
                val currentRoute by viewModel.currentRoute.collectAsState()

                // Define which screens should show top header & bottom nav elements
                val showBars = remember(currentRoute) {
                    currentRoute in listOf("dashboard", "pantry", "scanner", "recipes", "shopping_list")
                }

                // Temporary detail parameter states
                var activeIngredientId by remember { mutableStateOf<Int?>(null) }
                val activeRecipe by viewModel.selectedRecipe.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (showBars) {
                            val headerTitle = when (currentRoute) {
                                "dashboard" -> "ShelfLife"
                                "pantry" -> "Pantry Inventory"
                                "scanner" -> "Barcode Scanner"
                                "recipes" -> "Smart Recipes"
                                "shopping_list" -> "Shopping List"
                                else -> "ShelfLife"
                            }
                            ShelfLifeTopBar(
                                title = headerTitle,
                                onAvatarClick = { viewModel.navigateTo("settings") },
                                onSettingsClick = { viewModel.navigateTo("settings") }
                            )
                        }
                    },
                    bottomBar = {
                        if (showBars) {
                            ShelfLifeBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when {
                            currentRoute == "splash" -> {
                                SplashScreen(viewModel = viewModel)
                            }
                            currentRoute == "onboarding_1" -> {
                                OnboardingScreenOne(viewModel = viewModel)
                            }
                            currentRoute == "onboarding_2" -> {
                                OnboardingScreenTwo(viewModel = viewModel)
                            }
                            currentRoute == "onboarding_3" -> {
                                OnboardingScreenThree(viewModel = viewModel)
                            }
                            currentRoute == "dashboard" -> {
                                DashboardScreen(viewModel = viewModel)
                            }
                            currentRoute == "pantry" -> {
                                PantryScreen(
                                    viewModel = viewModel,
                                    onNavigateToDetail = { id ->
                                        activeIngredientId = id
                                        viewModel.navigateTo("ingredient_detail")
                                    },
                                    onNavigateToAdd = {
                                        viewModel.navigateTo("add_ingredient")
                                    }
                                )
                            }
                            currentRoute == "add_ingredient" -> {
                                AddIngredientScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo("pantry") }
                                )
                            }
                            currentRoute == "ingredient_detail" && activeIngredientId != null -> {
                                IngredientDetailScreen(
                                    viewModel = viewModel,
                                    id = activeIngredientId!!,
                                    onBack = { viewModel.navigateTo("pantry") },
                                    onEdit = { _ ->
                                        // Simple mockup fallback
                                        viewModel.navigateTo("pantry")
                                    }
                                )
                            }
                            currentRoute == "scanner" -> {
                                ScannerScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddManual = { viewModel.navigateTo("add_ingredient") }
                                )
                            }
                            currentRoute == "recipes" -> {
                                RecipesScreen(
                                    viewModel = viewModel,
                                    onNavigateToRecipeDetail = { recipe ->
                                        viewModel.selectRecipe(recipe)
                                        viewModel.navigateTo("recipe_detail")
                                    },
                                    onNavigateToChat = {
                                        viewModel.navigateTo("chat")
                                    }
                                )
                            }
                            currentRoute == "recipe_detail" && activeRecipe != null -> {
                                RecipeDetailScreen(
                                    viewModel = viewModel,
                                    recipe = activeRecipe!!,
                                    onBack = { viewModel.navigateTo("recipes") }
                                )
                            }
                            // Direct matching for recomended Today
                            currentRoute.startsWith("recipe_detail/") -> {
                                val rId = currentRoute.substringAfter("/")
                                val matched = viewModel.suggestedRecipes.value.firstOrNull { it.id == rId }
                                if (matched != null) {
                                    RecipeDetailScreen(
                                        viewModel = viewModel,
                                        recipe = matched,
                                        onBack = { viewModel.navigateTo("dashboard") }
                                    )
                                } else {
                                    viewModel.navigateTo("dashboard")
                                }
                            }
                            currentRoute == "chat" -> {
                                ChatScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo("recipes") }
                                )
                            }
                            currentRoute == "shopping_list" -> {
                                ShoppingListScreen(viewModel = viewModel)
                            }
                            currentRoute == "settings" -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { viewModel.navigateTo("dashboard") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
