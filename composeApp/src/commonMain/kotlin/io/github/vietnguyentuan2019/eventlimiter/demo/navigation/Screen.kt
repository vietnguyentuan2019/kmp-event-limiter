package io.github.vietnguyentuan2019.eventlimiter.demo.navigation

sealed class Screen(val route: String, val title: String, val icon: String) {
    data object Home : Screen("home", "Home", "🏠")
    data object Search : Screen("search", "Search Demo", "🔍")
    data object Form : Screen("form", "Form Validation", "📝")
    data object Payment : Screen("payment", "Payment Lock", "💳")
    data object Scroll : Screen("scroll", "Infinite Scroll", "📜")
    data object Settings : Screen("settings", "Settings", "⚙️")

    companion object {
        val allScreens = listOf(Home, Search, Form, Payment, Scroll, Settings)
    }
}
