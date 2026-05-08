package kryptonbutterfly.checklist.persistence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.database.getStringOrNull
import java.io.File
import java.io.FileOutputStream
import kryptonbutterfly.checklist.misc.scale
import java.util.UUID

private const val ICON_CACHE_FOLDER: String = "icon_cache/"
private fun iconCacheFolder(context: Context): File {
	return File(context.filesDir, ICON_CACHE_FOLDER)
}

private fun loadIconCache(context: Context): IconCache {
	val iconCache = IconCache()
	iconCache.loadIcons(context)
	return iconCache
}

private var cache: IconCache? = null
fun cache(context: Context): IconCache {
	return cache ?: loadIconCache(context).also { cache = it }
}

private const val TAG = "ICON_CACHE"

data class IconCache(val iconMap: HashMap<String, Bitmap> = HashMap()) {
	/**
	 * delete all unused icons
	 */
	fun cleanUp(context: Context, data: Data) {
		Log.i(TAG, "cleanUp")
		val usedIcons = data.categories.values.mapNotNull { it.icon }.toSet()
		val unusedIcons = iconMap.keys.filter { name -> !usedIcons.contains(name) }.toSet()
		Log.d(TAG, "deleting: $unusedIcons")
		val folder = iconCacheFolder(context)
		unusedIcons.forEach { name ->
			iconMap.remove(name)
			val file = File(folder, name)
			if (file.isFile && file.exists()) file.delete()
		}
	}
	fun addIcon(context: Context, uri: Uri, targetWidth: Int, targetHeight: Int): String? {
		context.contentResolver.query(
			uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
		)?.use {
			if (!it.moveToFirst()) return null
			val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
			it.getStringOrNull(nameIndex)?.also { name ->
				context.contentResolver.openInputStream(uri).use { iStream ->
					BitmapFactory.decodeStream(iStream)?.also { bitmap ->
						val scaled = scaleToFit(bitmap, targetWidth, targetHeight)
						return exists(scaled) ?: addIcon(context, name, scaled)
					}
				}
			}
		}
		return null
	}
	
	private fun exists(icon: Bitmap) =
		iconMap.entries.firstOrNull { (_, v) -> icon.sameAs(v) }?.let { (k, v) ->
			Log.d(TAG, "icon already known as: $k")
			return@let k
		}
	private fun scaleToFit(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
		val scaleWidth = maxWidth.toFloat() / source.width.toFloat()
		val scaleHeight = maxHeight.toFloat() / source.height.toFloat()
		if (scaleWidth >= 1 && scaleHeight >= 1) return source
		
		val scale = if (scaleWidth > scaleHeight) scaleHeight else scaleWidth
		return scale(source, scale)
	}
	
	private fun addIcon(context: Context, name: String, icon: Bitmap): String {
		val folder = iconCacheFolder(context)
		if (!folder.exists()) folder.mkdirs()
		val format: Bitmap.CompressFormat
		val iconName: String
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
		
		val fileName = makeUnique(iconName, format)
		val file = File(folder, fileName)
		FileOutputStream(file).use { oStream ->
			icon.compress(format, 100, oStream)
		}
		iconMap[fileName] = icon
		return fileName
	}
	
	private fun makeUnique(name: String, format: Bitmap.CompressFormat): String {
		val ext = ".${format.name.lowercase()}"
		var result = "$name$ext"
		while (iconMap.containsKey(result)) {
			val uuid = UUID.randomUUID()
			val lsb = uuid.leastSignificantBits.toULong().toString(32).uppercase()
			val msb = uuid.mostSignificantBits.toULong().toString(32).uppercase()
			result = "${name}_$lsb-$msb$ext"
		}
		return result
	}
	
	fun loadIcons(context: Context) {
		val folder = iconCacheFolder(context)
		if (!folder.exists()) return
		folder.listFiles { it.isFile }?.forEach { file ->
			val icon = BitmapFactory.decodeFile(file.absolutePath)
			this.iconMap[file.name] = icon
		}
	}
}
