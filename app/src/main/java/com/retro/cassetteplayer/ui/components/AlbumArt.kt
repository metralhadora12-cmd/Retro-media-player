package com.retro.cassetteplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.retro.cassetteplayer.ui.theme.LabelCream
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.InkSurface
import com.retro.cassetteplayer.ui.theme.InkRaised

/** Album art thumbnail; a small cassette shows through when a track has no embedded art. */
@Composable
fun AlbumArt(
    uri: Uri?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(InkRaised, InkSurface))),
        contentAlignment = Alignment.Center,
    ) {
        CassetteGlyph(Modifier.fillMaxSize(0.66f))
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

/**
 * Placeholder for songs without cover art: a tiny compact cassette (grey shell, cream
 * label with an orange stripe and two reels) so it matches the rest of the app.
 */
@Composable
fun CassetteGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1.55f)) {
        val w = size.width
        val h = size.height

        // Shell
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF4A4A4A), Color(0xFF363636))),
            cornerRadius = CornerRadius(w * 0.07f),
        )
        // Label with orange stripe
        val labelTopLeft = Offset(w * 0.08f, h * 0.1f)
        val labelSize = Size(w * 0.84f, h * 0.58f)
        drawRoundRect(LabelCream.copy(alpha = 0.9f), labelTopLeft, labelSize, CornerRadius(w * 0.03f))
        drawRect(TapeOrange, Offset(labelTopLeft.x, h * 0.53f), Size(labelSize.width, h * 0.09f))

        // Window with the two reels
        val window = Size(w * 0.5f, h * 0.24f)
        val windowTopLeft = Offset((w - window.width) / 2f, h * 0.25f)
        drawRoundRect(Color(0xFF151515), windowTopLeft, window, CornerRadius(window.height / 2f))
        val reelY = windowTopLeft.y + window.height / 2f
        val reelRadius = window.height * 0.36f
        listOf(w * 0.37f, w * 0.63f).forEach { x ->
            drawCircle(Color(0xFFB8B8B8), reelRadius, Offset(x, reelY), style = Stroke(reelRadius * 0.45f))
        }

        // Bottom head guide
        val guide = Path().apply {
            moveTo(w * 0.24f, h)
            lineTo(w * 0.3f, h * 0.78f)
            lineTo(w * 0.7f, h * 0.78f)
            lineTo(w * 0.76f, h)
            close()
        }
        drawPath(guide, Color(0xFF2A2A2A))
    }
}
