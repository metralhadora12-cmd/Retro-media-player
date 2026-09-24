package com.retro.cassetteplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.retro.cassetteplayer.ui.theme.CreamMuted
import com.retro.cassetteplayer.ui.theme.Graphite
import com.retro.cassetteplayer.ui.theme.Gunmetal

/** Album art thumbnail; the placeholder stays visible when a track has no embedded art. */
@Composable
fun AlbumArt(
    uri: Uri?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(Gunmetal, Graphite))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Album,
            contentDescription = null,
            tint = CreamMuted.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxSize(0.6f),
        )
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
