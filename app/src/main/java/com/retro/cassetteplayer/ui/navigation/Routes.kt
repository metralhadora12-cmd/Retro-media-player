package com.retro.cassetteplayer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import com.retro.cassetteplayer.ui.components.BottomDestination

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val COLLECTIONS = "collections"
    const val PLAYER = "player"

    val bottomDestinations = listOf(
        BottomDestination(HOME, "Home", Icons.Rounded.Home),
        BottomDestination(SEARCH, "Search", Icons.Rounded.Search),
        BottomDestination(COLLECTIONS, "Collections", Icons.Rounded.LibraryMusic),
    )
}
