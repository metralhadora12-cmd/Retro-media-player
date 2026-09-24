package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.cassetteplayer.data.CollectionKind
import com.retro.cassetteplayer.data.SongCollection
import com.retro.cassetteplayer.ui.theme.Charcoal
import com.retro.cassetteplayer.ui.theme.Cream
import com.retro.cassetteplayer.ui.theme.CreamMuted
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.MetalDark
import com.retro.cassetteplayer.ui.theme.RetroAmber
import com.retro.cassetteplayer.ui.theme.RetroOrange

/** Gradient that tints the top of Home / Library, like YouTube Music's header glow. */
val TopGlow = Brush.verticalGradient(
    listOf(Color(0xFF5C2408), Color(0xFF2C170C), Charcoal)
)

/** Filter chip styled like a small metal selector; selected chips turn cream. */
@Composable
fun RetroChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(chipBrush(selected))
            .border(1.dp, if (selected) RetroOrange else Color.White.copy(alpha = 0.1f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
            color = if (selected) Charcoal else Cream,
            maxLines = 1,
        )
    }
}

@Composable
fun RetroIconChip(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(chipBrush(selected = true))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Charcoal, modifier = Modifier.size(22.dp))
    }
}

private fun chipBrush(selected: Boolean): Brush =
    if (selected) Brush.verticalGradient(listOf(Cream, Color(0xFFE0CBA2)))
    else Brush.verticalGradient(listOf(Color(0xFF3A3A3A), Color(0xFF2A2A2A)))

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    leading: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    text = overline.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CreamMuted,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = Cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        action?.invoke()
    }
}

/** Small metal pill, used for "Tocar tudo". */
@Composable
fun MetalPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MetalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp))
    }
}

/** Square cover with the title over a dark fade and an orange "tape label" stripe. */
@Composable
fun CoverTile(
    collection: SongCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick),
    ) {
        CollageArt(collection.artworkUris, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 2.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = collection.title,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                color = Cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (collection.kind == CollectionKind.PLAYLIST) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Cream,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(RetroOrange, RetroAmber)))
        )
    }
}

/** Page indicator made of little LEDs. */
@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (i == current) RetroOrange else MetalDark, CircleShape)
            )
        }
    }
}

private fun SongCollection.artShape(corner: Int = 8): Shape =
    if (kind == CollectionKind.ARTIST) CircleShape else RoundedCornerShape(corner.dp)

private fun SongCollection.kindIcon(): ImageVector = when (kind) {
    CollectionKind.PLAYLIST -> Icons.AutoMirrored.Rounded.QueueMusic
    CollectionKind.ALBUM -> Icons.Rounded.Album
    CollectionKind.ARTIST -> Icons.Rounded.Person
}

/** Library grid cell: cover (round for artists), title and "Álbum • Artista • N faixas". */
@Composable
fun CollectionGridItem(
    collection: SongCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
    ) {
        CollageArt(
            uris = collection.artworkUris,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = collection.artShape(),
        )
        Text(
            text = collection.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Cream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                collection.kindIcon(),
                contentDescription = null,
                tint = RetroAmber,
                modifier = Modifier
                    .padding(top = 2.dp, end = 4.dp)
                    .size(14.dp),
            )
            Text(
                text = collection.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CreamMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CollectionListItem(
    collection: SongCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollageArt(collection.artworkUris, Modifier.size(56.dp), collection.artShape(6))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                collection.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                collection.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CreamMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
