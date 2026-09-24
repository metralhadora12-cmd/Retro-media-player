package com.retro.cassetteplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
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
    shape: Shape = RoundedCornerShape(6.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
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

/** Single cover, or a 2x2 collage when four covers are available (like playlist art). */
@Composable
fun CollageArt(
    uris: List<Uri>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    if (uris.size < 4) {
        AlbumArt(uris.firstOrNull(), modifier, shape)
        return
    }
    Column(modifier.clip(shape)) {
        for (row in 0 until 2) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (col in 0 until 2) {
                    AlbumArt(
                        uri = uris[row * 2 + col],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RectangleShape,
                    )
                }
            }
        }
    }
}
