package com.retro.cassetteplayer.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The most characteristic colour of a cover (vivid and common), or null when there is
 * no cover or it is basically grey.
 */
@Composable
fun rememberCoverColor(uri: Uri?, enabled: Boolean): Color? {
    val context = LocalContext.current
    var color by remember(uri, enabled) { mutableStateOf<Color?>(null) }
    LaunchedEffect(uri, enabled) {
        if (uri == null || !enabled) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(64)
            .allowHardware(false)
            .build()
        val bitmap = (context.imageLoader.execute(request) as? SuccessResult)
            ?.let { (it.drawable as? BitmapDrawable)?.bitmap }
            ?: return@LaunchedEffect
        color = withContext(Dispatchers.Default) { dominantColor(bitmap) }
    }
    return color
}

/** Hue histogram weighted by saturation and brightness; returns the winning bucket's average. */
private fun dominantColor(bitmap: Bitmap): Color? {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return null
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val buckets = 24
    val weight = FloatArray(buckets)
    val r = FloatArray(buckets)
    val g = FloatArray(buckets)
    val b = FloatArray(buckets)
    val hsv = FloatArray(3)
    for (p in pixels) {
        val red = (p shr 16) and 0xFF
        val green = (p shr 8) and 0xFF
        val blue = p and 0xFF
        android.graphics.Color.RGBToHSV(red, green, blue, hsv)
        val sat = hsv[1]
        val value = hsv[2]
        if (sat < 0.2f || value < 0.15f) continue
        val bucket = ((hsv[0] / 360f) * buckets).toInt().coerceIn(0, buckets - 1)
        val wgt = sat * value
        weight[bucket] += wgt
        r[bucket] += red * wgt
        g[bucket] += green * wgt
        b[bucket] += blue * wgt
    }
    val best = weight.indices.maxByOrNull { weight[it] } ?: return null
    // Ignore covers where colour is only a few stray pixels
    if (weight[best] < pixels.size * 0.03f) return null
    return Color(r[best] / weight[best] / 255f, g[best] / weight[best] / 255f, b[best] / weight[best] / 255f)
}
