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

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val PLAYER = "player"
    const val COLLECTION_ARG = "id"
    const val COLLECTION = "collection?$COLLECTION_ARG={$COLLECTION_ARG}"

    fun collection(id: String) = "collection?$COLLECTION_ARG=${Uri.encode(id)}"

    val bottomDestinations = listOf(
        BottomDestination(HOME, "Início", Icons.Outlined.Home, Icons.Filled.Home),
        BottomDestination(SEARCH, "Buscar", Icons.Outlined.Search, Icons.Filled.Search),
        BottomDestination(LIBRARY, "Biblioteca", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
    )
}
