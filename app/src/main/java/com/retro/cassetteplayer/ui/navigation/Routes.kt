package com.retro.cassetteplayer.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import com.retro.cassetteplayer.ui.components.BottomDestination
import com.retro.cassetteplayer.R

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
    const val EQUALIZER = "equalizer"
    const val CHANGELOG = "changelog"
    const val COLLECTION_ARG = "id"
    const val COLLECTION = "collection?$COLLECTION_ARG={$COLLECTION_ARG}"

    fun collection(id: String) = "collection?$COLLECTION_ARG=${Uri.encode(id)}"

    val bottomDestinations = listOf(
        BottomDestination(HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
        BottomDestination(SEARCH, R.string.nav_search, Icons.Outlined.Search, Icons.Filled.Search),
        BottomDestination(LIBRARY, R.string.nav_library, Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
    )
}
