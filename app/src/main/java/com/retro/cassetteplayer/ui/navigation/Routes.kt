package com.retro.cassetteplayer.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
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
        BottomDestination(HOME, "Início", Icons.Rounded.Home),
        BottomDestination(SEARCH, "Buscar", Icons.Rounded.Search),
        BottomDestination(LIBRARY, "Biblioteca", Icons.Rounded.LibraryMusic),
    )
}
