package kryptonbutterfly.checklist.persistence

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.database.getStringOrNull
import java.io.File
import java.io.FileOutputStream

private const val ICON_CACHE_FOLDER: String = "icon_cache/"

private fun loadIconCache(context: ContextWrapper): IconCache {
	val folder = File(context.filesDir, ICON_CACHE_FOLDER)
	val iconCache = IconCache()
	if (folder.exists())
		iconCache.loadIcons(context)
	return iconCache
}

private var cache : IconCache? = null
fun cache(context: ContextWrapper) : IconCache {
	return cache ?: loadIconCache(context).also { cache = it }
}

data class IconCache(val iconMap: HashMap<String, Bitmap> = HashMap()) {
	fun addIcon(context: ContextWrapper, uri: Uri): String? {
		context.contentResolver.query(uri,
			arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.
			use {
				if (!it.moveToFirst())
					return null
				val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
				it.getStringOrNull(nameIndex)?.also { name ->
					context.contentResolver.openInputStream(uri).use { iStream ->
						val bitmap = BitmapFactory.decodeStream(iStream)
						return addIcon(context, name, bitmap)
					}
				}
			}
		return null
	}
	
	private fun addIcon(context: ContextWrapper, name: String, icon: Bitmap): String {
		val folder = File(context.filesDir, ICON_CACHE_FOLDER)
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
	
	fun loadIcons(context: ContextWrapper) {
		val folder = File(context.filesDir, ICON_CACHE_FOLDER)
		if (!folder.exists())
			return
		folder.listFiles { it.isFile } ?.forEach { file -> this.iconMap.put(file.name, BitmapFactory.decodeFile(file.absolutePath)) }
	}
}
