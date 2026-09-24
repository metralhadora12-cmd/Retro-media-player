package com.retro.cassetteplayer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.retro.cassetteplayer.playback.PlaybackState
import com.retro.cassetteplayer.ui.components.PianoKey
import com.retro.cassetteplayer.ui.components.PianoKeys
import com.retro.cassetteplayer.ui.components.RetroSeekBar
import com.retro.cassetteplayer.ui.components.VERTICAL_CASSETTE_ASPECT
import com.retro.cassetteplayer.ui.components.VerticalCassette
import com.retro.cassetteplayer.ui.theme.PlayerGlow
import com.retro.cassetteplayer.ui.theme.DisplayFont
import com.retro.cassetteplayer.ui.theme.TapeOrange
import com.retro.cassetteplayer.ui.theme.TextPrimary
import com.retro.cassetteplayer.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.ui.components.artistLabel
import com.retro.cassetteplayer.ui.components.titleLabel
import com.retro.cassetteplayer.playback.AudioFormatInfo
import com.retro.cassetteplayer.ui.components.HqBadge

@Composable
fun PlayerScreen(
    playback: PlaybackState,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    onSaveToPlaylist: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerGlow)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
            }
            Text(
                text = stringResource(R.string.player_now_playing),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = DisplayFont, letterSpacing = 2.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSaveToPlaylist, enabled = playback.hasMedia) {
                Icon(
                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = stringResource(R.string.menu_save_to_playlist),
                    tint = if (playback.hasMedia) TextPrimary else TextSecondary,
                )
            }
        }

        // The cassette takes whatever height is left, keeping its upright proportions.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            VerticalCassette(
                isPlaying = playback.isPlaying,
                progress = playback.progress,
                title = if (playback.hasMedia) titleLabel(playback.title) else "",
                subtitle = if (playback.hasMedia) artistLabel(playback.artist) else "",
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(VERTICAL_CASSETTE_ASPECT, matchHeightConstraintsFirst = true)
                    .shadow(28.dp, RoundedCornerShape(percent = 3), spotColor = TapeOrange.copy(alpha = 0.5f)),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeToggle(
                icon = Icons.Rounded.Shuffle,
                description = stringResource(R.string.action_shuffle),
                active = playback.shuffleEnabled,
                onClick = onToggleShuffle,
            )
            PianoKeys(
                keys = listOf(
                    PianoKey(Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous), onPrevious),
                    PianoKey(
                        icon = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        description = stringResource(if (playback.isPlaying) R.string.action_pause else R.string.action_play),
                        onClick = onTogglePlay,
                        latched = playback.isPlaying,
                        weight = 1.3f,
                    ),
                    PianoKey(Icons.Rounded.SkipNext, stringResource(R.string.player_next), onNext),
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            ModeToggle(
                icon = if (playback.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                description = stringResource(R.string.player_repeat),
                active = playback.repeatMode != Player.REPEAT_MODE_OFF,
                onClick = onCycleRepeat,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = if (playback.hasMedia) titleLabel(playback.title) else stringResource(R.string.player_no_tape),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .padding(vertical = 8.dp)
                .size(width = 28.dp, height = 2.dp)
                .background(TapeOrange, RoundedCornerShape(1.dp))
        )
        Text(
            text = if (playback.hasMedia) artistLabel(playback.artist) else "",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        playback.audioFormat?.takeIf { playback.hasMedia }?.let { AudioFormatLine(it) }

        Spacer(Modifier.height(16.dp))

        RetroSeekBar(
            positionMs = playback.positionMs,
            durationMs = playback.durationMs,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )

        // Bottom row: heart (favourite) on the left, "A SEGUIR" and "LETRA" tabs in the middle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FavoriteButton(
                isFavorite = isFavorite,
                enabled = playback.hasMedia,
                onClick = onToggleFavorite,
            )
            Spacer(Modifier.weight(1f))
            PlayerTab(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.player_up_next), onOpenQueue)
            PlayerTab(Icons.Rounded.Lyrics, stringResource(R.string.player_lyrics), onOpenLyrics, enabled = playback.hasMedia)
            Spacer(Modifier.weight(1f))
            // Balances the heart so the tabs stay centred
            Spacer(Modifier.size(48.dp))
        }
    }
}

/** Text tab at the bottom of the player, like YouTube Music's "A SEGUIR" / "LETRA". */
@Composable
private fun PlayerTab(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f)
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp), color = color)
    }
}

/** Heart that fills in tape orange and does a small bounce when liked. */
@Composable
private fun FavoriteButton(isFavorite: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    var bounceKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(bounceKey) {
        if (bounceKey > 0) {
            scale.animateTo(1.3f, tween(110))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!isFavorite) bounceKey++
            onClick()
        },
        enabled = enabled,
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(if (isFavorite) R.string.menu_remove_favorite else R.string.menu_add_favorite),
            tint = when {
                !enabled -> TextSecondary.copy(alpha = 0.4f)
                isFavorite -> TapeOrange
                else -> TextPrimary
            },
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

/** Small printed icon with an orange indicator dot under it when the mode is on. */
@Composable
private fun ModeToggle(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (active) TapeOrange else TextSecondary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .size(5.dp)
                .background(if (active) TapeOrange else Color.Transparent, CircleShape)
        )
    }
}

/** "[HQ] FLAC · 24-bit · 96 kHz" under the artist, from the decoded track. */
@Composable
private fun AudioFormatLine(format: AudioFormatInfo) {
    val parts = buildList {
        add(format.codec)
        if (format.bitDepth > 0) add(stringResource(R.string.audio_bits, format.bitDepth))
        if (format.sampleRate > 0) add(stringResource(R.string.audio_khz, formatKhz(format.sampleRate)))
    }
    Row(
        modifier = Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (format.lossless) {
            HqBadge(Modifier.padding(end = 8.dp))
        }
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = DisplayFont),
            color = TextSecondary,
        )
    }
}

private fun formatKhz(hz: Int): String =
    String.format(java.util.Locale.ROOT, "%.1f", hz / 1000f).removeSuffix(".0")

