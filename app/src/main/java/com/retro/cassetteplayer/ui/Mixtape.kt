package com.retro.cassetteplayer.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextUtils
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.retro.cassetteplayer.R
import com.retro.cassetteplayer.data.AppSettings
import com.retro.cassetteplayer.data.CassetteModel
import com.retro.cassetteplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * "Compartilhar mixtape": draws a cassette with the playlist name handwritten on the
 * label and the track list split into side A and side B, then opens the share sheet.
 */
object Mixtape {

    suspend fun share(context: Context, title: String, songs: List<Song>): Boolean {
        val file = withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = render(context, title, songs)
                val dir = File(context.cacheDir, "share").apply { mkdirs() }
                File(dir, "mixtape.png").also { f ->
                    f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }.getOrNull()
        } ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.menu_share_mixtape))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    private fun render(context: Context, title: String, songs: List<Song>): Bitmap {
        val width = 1080
        val height = 1500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val marker = runCatching { ResourcesCompat.getFont(context, R.font.permanent_marker) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val band = AppSettings.labelColor.value.argb.toInt()
        val model = AppSettings.cassetteModel.value

        // Backdrop
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), 0xFF2E1A10.toInt(), 0xFF0B0B0B.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Cassette shell
        val shell = RectF(90f, 90f, 990f, 660f)
        paint.color = when (model) {
            CassetteModel.METAL -> 0xFF101012.toInt()
            CassetteModel.CHROME -> 0xFF2A2D33.toInt()
            CassetteModel.NORMAL -> 0xFF1B1B1D.toInt()
        }
        canvas.drawRoundRect(shell, 36f, 36f, paint)
        paint.color = 0xFF000000.toInt()
        listOf(shell.left + 30f to shell.top + 30f, shell.right - 30f to shell.top + 30f, shell.left + 30f to shell.bottom - 30f, shell.right - 30f to shell.bottom - 30f)
            .forEach { (x, y) -> paint.color = 0xFF5A5D63.toInt(); canvas.drawCircle(x, y, 12f, paint) }

        // Label
        val label = RectF(shell.left + 60f, shell.top + 50f, shell.right - 60f, shell.top + 400f)
        paint.shader = when (model) {
            CassetteModel.NORMAL -> LinearGradient(label.left, 0f, label.right, 0f, 0xFFECE7DD.toInt(), 0xFFDDD7CB.toInt(), Shader.TileMode.CLAMP)
            CassetteModel.CHROME -> LinearGradient(label.left, 0f, label.right, 0f, 0xFFE6EAEF.toInt(), 0xFFAAB2BC.toInt(), Shader.TileMode.CLAMP)
            CassetteModel.METAL -> LinearGradient(label.left, 0f, label.right, 0f, 0xFF2B2C30.toInt(), 0xFF121315.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(label, 18f, 18f, paint)
        paint.shader = null
        val ink = if (model == CassetteModel.METAL) 0xFFD8B56A.toInt() else 0xFF26282C.toInt()
        // Coloured stripe across the lower part of the label
        paint.color = band
        canvas.drawRect(label.left, label.top + 250f, label.right, label.top + 320f, paint)

        // Handwritten title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = marker
            color = ink
            textSize = 64f
        }
        val shownTitle = TextUtils.ellipsize(title, titlePaint, label.width() - 80f, TextUtils.TruncateAt.END).toString()
        canvas.drawText(shownTitle, label.left + 40f, label.top + 95f, titlePaint)
        paint.color = ink
        paint.strokeWidth = 3f
        canvas.drawLine(label.left + 40f, label.top + 125f, label.right - 40f, label.top + 125f, paint)
        canvas.drawLine(label.left + 40f, label.top + 175f, label.right - 40f, label.top + 175f, paint)

        // Window with the reels
        val window = RectF(label.left + 170f, label.top + 150f, label.right - 170f, label.top + 290f)
        paint.color = 0xFF0A0A0B.toInt()
        canvas.drawRoundRect(window, 70f, 70f, paint)
        listOf(window.left + 90f, window.right - 90f).forEach { cx ->
            val cy = window.centerY()
            paint.color = 0xFF3B2618.toInt()
            canvas.drawCircle(cx, cy, 58f, paint)
            paint.color = 0xFFBFC2C8.toInt()
            canvas.drawCircle(cx, cy, 30f, paint)
            paint.color = 0xFF1A1B1E.toInt()
            canvas.drawCircle(cx, cy, 20f, paint)
            paint.color = 0xFFBFC2C8.toInt()
            for (i in 0 until 6) {
                val angle = Math.toRadians(i * 60.0)
                canvas.drawCircle(cx + (22 * kotlin.math.cos(angle)).toFloat(), cy + (22 * kotlin.math.sin(angle)).toFloat(), 4f, paint)
            }
        }
        paint.color = 0xFF3B2618.toInt()
        canvas.drawRect(window.left + 90f, window.centerY() + 50f, window.right - 90f, window.centerY() + 58f, paint)

        // Tape guide at the bottom of the shell
        paint.color = 0xFF2A2A2E.toInt()
        val path = android.graphics.Path().apply {
            moveTo(shell.left + 200f, shell.bottom)
            lineTo(shell.left + 250f, shell.bottom - 110f)
            lineTo(shell.right - 250f, shell.bottom - 110f)
            lineTo(shell.right - 200f, shell.bottom)
            close()
        }
        canvas.drawPath(path, paint)

        // Track list, side A and side B
        val sideB = (songs.size + 1) / 2
        val listTop = 760f
        val headPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            color = band
            textSize = 34f
            letterSpacing = 0.2f
        }
        val trackPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT
            color = 0xFFFFFFFF.toInt()
            textSize = 30f
        }
        val artistPaint = TextPaint(trackPaint).apply { color = 0xFFAAAAAA.toInt(); textSize = 24f }
        val columnWidth = 440f
        listOf(
            context.getString(R.string.mixtape_side_a) to songs.take(sideB),
            context.getString(R.string.mixtape_side_b) to songs.drop(sideB),
        ).forEachIndexed { column, (heading, tracks) ->
            val x = 90f + column * (columnWidth + 60f)
            canvas.drawText(heading, x, listTop, headPaint)
            var y = listTop + 60f
            val shown = tracks.take(MAX_PER_SIDE)
            shown.forEachIndexed { i, song ->
                val name = "${i + 1}. ${song.title.ifBlank { context.getString(R.string.unknown_title) }}"
                canvas.drawText(TextUtils.ellipsize(name, trackPaint, columnWidth, TextUtils.TruncateAt.END).toString(), x, y, trackPaint)
                val artist = song.artist.ifBlank { context.getString(R.string.unknown_artist) }
                canvas.drawText(TextUtils.ellipsize(artist, artistPaint, columnWidth - 40f, TextUtils.TruncateAt.END).toString(), x + 40f, y + 32f, artistPaint)
                y += 74f
            }
            if (tracks.size > shown.size) {
                canvas.drawText(context.getString(R.string.mixtape_more, tracks.size - shown.size), x, y, artistPaint)
            }
        }

        val footer = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            color = 0xFF777777.toInt()
            textSize = 26f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }
        canvas.drawText(context.getString(R.string.mixtape_footer).uppercase(), width / 2f, height - 50f, footer)
        return bitmap
    }

    private const val MAX_PER_SIDE = 8
}
