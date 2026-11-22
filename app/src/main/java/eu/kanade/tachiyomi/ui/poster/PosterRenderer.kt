package eu.kanade.tachiyomi.ui.poster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.library.model.LibraryManga
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Menggambar poster komik ke satu Bitmap (dipakai untuk pratinjau DAN ekspor, jadi hasilnya sama).
 * Semua koordinat memakai kanvas dasar 1080x1920; hasil diskalakan sesuai lebar yang diminta.
 * Dibuat sendiri untuk KomKy (tidak menyalin kode dari aplikasi lain).
 */
object PosterRenderer {

    const val BASE_WIDTH = 1080
    private const val BASE_HEIGHT = 1920
    private const val CARD_W = 640f
    private const val CARD_H = 250f

    private class Slot(val fx: Float, val fy: Float, val angle: Float)

    // Urutan pemakaian slot: atas, bawah, kanan-atas, kiri-bawah, kiri-tengah.
    private val slots = listOf(
        Slot(0.34f, 0.11f, -10f),
        Slot(0.70f, 0.89f, -9f),
        Slot(0.68f, 0.24f, 8f),
        Slot(0.32f, 0.77f, 7f),
        Slot(0.20f, 0.50f, -14f),
    )

    private class Card(
        val title: String,
        val author: String,
        val read: Long,
        val total: Long,
        val cover: Bitmap?,
    )

    suspend fun render(
        context: Context,
        photoUri: Uri,
        comics: List<LibraryManga>,
        seed: Int,
        watermark: Boolean,
        width: Int,
    ): Bitmap {
        val photo = loadPhoto(context, photoUri)
        val cards = comics.map { item ->
            Card(
                title = item.manga.title,
                author = item.manga.author.orEmpty(),
                read = item.readCount,
                total = item.totalChapters,
                cover = loadCover(context, item),
            )
        }
        return withContext(Dispatchers.Default) { draw(photo, cards, seed, watermark, width) }
    }

    // ---------- Memuat gambar ----------

    private suspend fun loadCover(context: Context, item: LibraryManga): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(item.manga)
                .size(300, 450)
                .build()
            val drawable = context.imageLoader.execute(request).image?.asDrawable(context.resources)
            val bitmap = drawable?.getBitmapOrNull() ?: return null
            toSoftware(bitmap)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun toSoftware(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    }

    private suspend fun loadPhoto(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val sample = max(1, min(info.size.width / BASE_WIDTH, info.size.height / BASE_HEIGHT))
                decoder.setTargetSampleSize(sample)
            }
        } else {
            decodeLegacy(context, uri)
        }
    }

    private fun decodeLegacy(context: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= BASE_WIDTH && bounds.outHeight / (sample * 2) >= BASE_HEIGHT) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalStateException("Foto tidak bisa dibaca")

        val degrees = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (degrees == 0f) return decoded

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }

    // ---------- Menggambar ----------

    private fun draw(photo: Bitmap, cards: List<Card>, seed: Int, watermark: Boolean, width: Int): Bitmap {
        val scale = width / BASE_WIDTH.toFloat()
        val height = (BASE_HEIGHT * scale).toInt()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.scale(scale, scale)

        drawBackground(canvas, photo)

        val random = Random(seed)
        cards.shuffled(random).forEachIndexed { index, card ->
            val slot = slots[index % slots.size]
            val cx = slot.fx * BASE_WIDTH + random.nextInt(-14, 15)
            val cy = slot.fy * BASE_HEIGHT + random.nextInt(-14, 15)
            val angle = slot.angle + random.nextInt(-20, 21) / 10f
            drawCard(canvas, card, cx, cy, angle)
        }

        if (watermark) drawWatermark(canvas)
        return output
    }

    private fun drawBackground(canvas: Canvas, photo: Bitmap) {
        val w = BASE_WIDTH.toFloat()
        val h = BASE_HEIGHT.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Center-crop foto agar memenuhi seluruh poster.
        val s = max(w / photo.width, h / photo.height)
        val cropW = w / s
        val cropH = h / s
        val left = (photo.width - cropW) / 2f
        val top = (photo.height - cropH) / 2f
        val src = Rect(left.toInt(), top.toInt(), (left + cropW).toInt(), (top + cropH).toInt())
        canvas.drawBitmap(photo, src, RectF(0f, 0f, w, h), paint)

        // Bayangan tipis di atas dan bawah supaya kartu lebih menonjol.
        val shade = Paint()
        shade.shader = LinearGradient(0f, 0f, 0f, 600f, Color.argb(120, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, 600f, shade)
        shade.shader = LinearGradient(0f, h - 600f, 0f, h, Color.TRANSPARENT, Color.argb(140, 0, 0, 0), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h - 600f, w, h, shade)
    }

    private fun drawCard(canvas: Canvas, card: Card, cx: Float, cy: Float, angle: Float) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angle)

        val rect = RectF(-CARD_W / 2f, -CARD_H / 2f, CARD_W / 2f, CARD_H / 2f)
        val radius = 44f

        // Latar kartu gelap (kaca) + bayangan.
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            Color.argb(235, 44, 38, 70), Color.argb(235, 18, 16, 30), Shader.TileMode.CLAMP,
        )
        fill.setShadowLayer(36f, 0f, 14f, Color.argb(140, 0, 0, 0))
        canvas.drawRoundRect(rect, radius, radius, fill)

        // Tepi berkilau.
        val border = Paint(Paint.ANTI_ALIAS_FLAG)
        border.style = Paint.Style.STROKE
        border.strokeWidth = 3f
        border.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            Color.argb(200, 255, 255, 255), Color.argb(40, 255, 255, 255), Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, radius, radius, border)

        // Sampul komik (kiri).
        val pad = 22f
        val coverRect = RectF(rect.left + pad, rect.top + pad, rect.left + pad + 138f, rect.bottom - pad)
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(coverRect, 14f, 14f, Path.Direction.CW) })
        val cover = card.cover
        if (cover != null) {
            val s = max(coverRect.width() / cover.width, coverRect.height() / cover.height)
            val cw = coverRect.width() / s
            val ch = coverRect.height() / s
            val l = (cover.width - cw) / 2f
            val t = (cover.height - ch) / 2f
            val src = Rect(l.toInt(), t.toInt(), (l + cw).toInt(), (t + ch).toInt())
            canvas.drawBitmap(cover, src, coverRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            val placeholder = Paint()
            placeholder.color = Color.argb(255, 70, 64, 100)
            canvas.drawRect(coverRect, placeholder)
        }
        canvas.restore()

        // Judul dan penulis.
        val textLeft = coverRect.right + 24f
        val textWidth = rect.right - pad - textLeft

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        titlePaint.color = Color.WHITE
        titlePaint.textSize = 38f
        titlePaint.typeface = Typeface.DEFAULT_BOLD
        val title = TextUtils.ellipsize(card.title, titlePaint, textWidth, TextUtils.TruncateAt.END).toString()
        canvas.drawText(title, textLeft, rect.top + 68f, titlePaint)

        if (card.author.isNotBlank()) {
            val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            authorPaint.color = Color.argb(190, 255, 255, 255)
            authorPaint.textSize = 28f
            val author = TextUtils.ellipsize(card.author, authorPaint, textWidth, TextUtils.TruncateAt.END).toString()
            canvas.drawText(author, textLeft, rect.top + 108f, authorPaint)
        }

        // Bar progres baca.
        val barTop = rect.top + 140f
        val barHeight = 10f
        val track = Paint(Paint.ANTI_ALIAS_FLAG)
        track.color = Color.argb(70, 255, 255, 255)
        canvas.drawRoundRect(RectF(textLeft, barTop, textLeft + textWidth, barTop + barHeight), 5f, 5f, track)
        val fraction = if (card.total > 0) (card.read.toFloat() / card.total.toFloat()).coerceIn(0f, 1f) else 0f
        if (fraction > 0f) {
            val progress = Paint(Paint.ANTI_ALIAS_FLAG)
            progress.color = Color.WHITE
            canvas.drawRoundRect(
                RectF(textLeft, barTop, textLeft + textWidth * fraction, barTop + barHeight),
                5f,
                5f,
                progress,
            )
        }

        // "Bab 45 / 120" + ikon hiasan (favorit, sebelumnya, berikutnya).
        val infoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        infoPaint.color = Color.argb(190, 255, 255, 255)
        infoPaint.textSize = 24f
        val baseline = rect.top + 190f
        canvas.drawText("Bab ${card.read} / ${card.total}", textLeft, baseline, infoPaint)

        val iconY = baseline - 8f
        val icon = Paint(Paint.ANTI_ALIAS_FLAG)
        icon.color = Color.WHITE
        val nextX = rect.right - pad - 40f
        val prevX = nextX - 62f
        val prev = Path().apply {
            moveTo(prevX + 22f, iconY - 16f)
            lineTo(prevX, iconY)
            lineTo(prevX + 22f, iconY + 16f)
            close()
        }
        val next = Path().apply {
            moveTo(nextX, iconY - 16f)
            lineTo(nextX + 22f, iconY)
            lineTo(nextX, iconY + 16f)
            close()
        }
        canvas.drawPath(prev, icon)
        canvas.drawPath(next, icon)

        val heart = TextPaint(Paint.ANTI_ALIAS_FLAG)
        heart.color = Color.argb(255, 255, 120, 150)
        heart.textSize = 32f
        canvas.drawText("\u2665", prevX - 60f, iconY + 11f, heart)

        canvas.restore()
    }

    private fun drawWatermark(canvas: Canvas) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(170, 255, 255, 255)
        paint.textSize = 40f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("KomKy", BASE_WIDTH / 2f, BASE_HEIGHT - 56f, paint)
    }
}
