package com.retro.cassetteplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.playback.PlaybackState
import com.retro.cassetteplayer.ui.components.AcrylicCase
import com.retro.cassetteplayer.ui.components.CassetteTape
import com.retro.cassetteplayer.ui.components.MetalButton
import com.retro.cassetteplayer.ui.components.MetalIconButton
import com.retro.cassetteplayer.ui.components.PlaybackLed
import com.retro.cassetteplayer.ui.components.RetroSeekBar
import com.retro.cassetteplayer.ui.components.steelBrushedMetal
import com.retro.cassetteplayer.ui.theme.Cream
import com.retro.cassetteplayer.ui.theme.CreamMuted

@Composable
fun PlayerScreen(
    playback: PlaybackState,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .steelBrushedMetal()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetalIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Voltar",
                onClick = onBack,
                modifier = Modifier.size(42.dp),
            )
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelLarge,
                color = Cream,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier.width(42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                PlaybackLed(lit = playback.isPlaying)
            }
        }

        Spacer(Modifier.weight(1f))

        AcrylicCase(Modifier.fillMaxWidth()) {
            CassetteTape(
                isPlaying = playback.isPlaying,
                progress = playback.progress,
                label = playback.title,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = playback.title.ifBlank { "Nenhuma fita inserida" },
            style = MaterialTheme.typography.titleLarge,
            color = Cream,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = playback.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = CreamMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(20.dp))

        RetroSeekBar(
            positionMs = playback.positionMs,
            durationMs = playback.durationMs,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        DeckControls(
            isPlaying = playback.isPlaying,
            onTogglePlay = onTogglePlay,
            onPrevious = onPrevious,
            onNext = onNext,
            onRewind = onRewind,
            onFastForward = onFastForward,
        )

        Spacer(Modifier.height(16.dp))

        MetalButton(
            onClick = onToggleShuffle,
            latched = playback.shuffleEnabled,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlaybackLed(lit = playback.shuffleEnabled)
                Spacer(Modifier.width(8.dp))
                Text("SHUFFLE", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.weight(0.5f))
    }
}

/** Row of piano-style deck keys set in a recessed panel. */
@Composable
private fun DeckControls(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
) {
    val panelShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f), panelShape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), panelShape)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeckKey(Icons.Rounded.SkipPrevious, "Faixa anterior", onPrevious)
        DeckKey(Icons.Rounded.FastRewind, "Retroceder 10 segundos", onRewind)
        DeckKey(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            description = if (isPlaying) "Pausar" else "Tocar",
            onClick = onTogglePlay,
            latched = isPlaying,
            primary = true,
        )
        DeckKey(Icons.Rounded.FastForward, "Avançar 10 segundos", onFastForward)
        DeckKey(Icons.Rounded.SkipNext, "Próxima faixa", onNext)
    }
}

@Composable
private fun RowScope.DeckKey(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    latched: Boolean = false,
    primary: Boolean = false,
) {
    MetalIconButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        modifier = Modifier
            .weight(if (primary) 1.4f else 1f)
            .height(if (primary) 70.dp else 62.dp),
        iconSize = if (primary) 34.dp else 26.dp,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
        latched = latched,
        accent = primary && latched,
    )
}
