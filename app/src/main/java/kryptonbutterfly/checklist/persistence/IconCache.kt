package kryptonbutterfly.checklist.persistence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.database.getStringOrNull
import java.io.File
import java.io.FileOutputStream
import kryptonbutterfly.checklist.misc.scale

private const val ICON_CACHE_FOLDER: String = "icon_cache/"
private fun iconCacheFolder(context: Context): File {
	return File(context.filesDir, ICON_CACHE_FOLDER)
}
private fun loadIconCache(context: Context): IconCache {
	val iconCache = IconCache()
	iconCache.loadIcons(context)
	return iconCache
}

private var cache : IconCache? = null
fun cache(context: Context) : IconCache {
	return cache ?: loadIconCache(context).also { cache = it }
}

data class IconCache(val iconMap: HashMap<String, Bitmap> = HashMap()) {
	fun addIcon(context: Context, uri: Uri, targetWidth: Int, targetHeight: Int): String? {
		context.contentResolver.query(uri,
			arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.
			use {
				if (!it.moveToFirst())
					return null
				val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
				it.getStringOrNull(nameIndex)?.also{ name ->
					context.contentResolver.openInputStream(uri).use { iStream ->
						BitmapFactory.decodeStream(iStream)?.also { bitmap ->
							val scaled = scaleToFit(bitmap, targetWidth, targetHeight)
							return addIcon(context, name, scaled)
						}
					}
				}
			}
		return null
	}
	
	private fun scaleToFit(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
		val scaleWidth = maxWidth.toFloat() / source.width.toFloat()
		val scaleHeight = maxHeight.toFloat() / source.height.toFloat()
		if (scaleWidth >= 1 && scaleHeight >= 1)
			return source
		
		val scale = if (scaleWidth > scaleHeight) scaleHeight else scaleWidth
		return scale(source, scale)
	}
	
	private fun addIcon(context: Context, name: String, icon: Bitmap): String {
		val folder = iconCacheFolder(context)
		if (!folder.exists())
			folder.mkdirs()
		val iconName: String
		val format: Bitmap.CompressFormat
		if (name.contains('.')) {
			val dot = name.lastIndexOf('.')
			iconName = name.substring(0, dot)
			val extension = name.substring(dot + 1)
			format = when (extension) {
				"png" -> Bitmap.CompressFormat.PNG
				"jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
				"webp" -> Bitmap.CompressFormat.WEBP
				else -> Bitmap.CompressFormat.PNG
			}
		} else {
			iconName = name
			format = Bitmap.CompressFormat.PNG
		}
		
		val fileName = "$iconName.${format.name.lowercase()}"
		val file = File(folder, fileName)
		FileOutputStream(file).use { oStream ->
			icon.compress(format, 100, oStream)
		}
		iconMap[fileName] = icon
		return fileName
	}
	
	fun loadIcons(context: Context) {
		val folder = iconCacheFolder(context)
		if (!folder.exists())
			return
		folder.listFiles { it.isFile } ?.forEach { file ->
			this.iconMap.put(file.name, BitmapFactory.decodeFile(file.absolutePath))
		}
	}
}
