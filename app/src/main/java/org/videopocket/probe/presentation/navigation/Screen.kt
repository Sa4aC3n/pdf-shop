package org.videopocket.probe.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Merge : Screen("merge")
    data object ImagesToPdf : Screen("images_to_pdf")
    data object Split : Screen("split")
}
