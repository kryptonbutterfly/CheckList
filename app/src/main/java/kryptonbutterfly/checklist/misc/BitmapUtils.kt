package kryptonbutterfly.checklist.misc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

fun scale(source: Bitmap, scale: Float): Bitmap {
	val targetWidth = (source.width * scale).roundToInt()
	val targetHeight = (source.height * scale).roundToInt()
	var result = source
	while (result.width > targetWidth) {
		if (targetWidth * 2 < result.width) {
			val w = (result.width * 0.5).roundToInt()
			val h = (result.height * 0.5).roundToInt()
			result = scaleIncrement(result, w, h)
		}
		else
			result = scaleIncrement(result, targetWidth, targetHeight)
	}
	return result
}

private fun scaleIncrement(source: Bitmap, width: Int, height: Int): Bitmap {
	val result = createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
	val canvas = Canvas(result)
	val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
	paint.isDither = true
	canvas.drawBitmap(source, null, Rect(0, 0, width, height), paint)
	return result
}