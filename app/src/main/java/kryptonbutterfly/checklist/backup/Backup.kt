package kryptonbutterfly.checklist.backup

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import kryptonbutterfly.checklist.Constants.GSON
import kryptonbutterfly.checklist.MainActivity
import kryptonbutterfly.checklist.persistence.ICON_CACHE_FOLDER
import kryptonbutterfly.checklist.persistence.SETTINGS_FILE
import kryptonbutterfly.checklist.persistence.TASKS_LIST_FILE
import kryptonbutterfly.checklist.persistence.cache
import kryptonbutterfly.checklist.persistence.data
import kryptonbutterfly.checklist.persistence.settings
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Backup {
	private const val RESTART_REQUEST_CODE = 11507 //IMPORTANT DO NOT CHANGE!
	fun create(context: Context, uri: Uri) {
		context.contentResolver.openOutputStream(uri)
			?.let { ZipOutputStream(it.buffered()) }
			?.use { zip ->
				zip.putNextEntry(ZipEntry(ICON_CACHE_FOLDER))
				zip.closeEntry()
				
				val cache = cache(context)
				cache.iconMap.forEach { (name, icon) ->
					val (_, format) = cache.getFormat(name)
					zip.putNextEntry(ZipEntry(ICON_CACHE_FOLDER + name))
					icon.compress(format, 100, zip)
					zip.closeEntry()
				}
				
				zip.putNextEntry(ZipEntry(TASKS_LIST_FILE))
				zip.write(GSON.toJson(data(context)).toByteArray(Charsets.UTF_8))
				zip.closeEntry()
				
				zip.putNextEntry(ZipEntry(SETTINGS_FILE))
				zip.write(GSON.toJson(settings(context)).toByteArray(Charsets.UTF_8))
				zip.closeEntry()
			}
	}
	
	private class RawContents(
		var settings: ByteArray? = null,
		var data: ByteArray? = null,
		val icons: HashMap<String, ByteArray> = HashMap(),
		var hasIcons: Boolean = false
	)
	{
		private fun isValid() = settings != null && data != null && hasIcons
		
		fun writeContent(context: Context) {
			if (isValid()) {
				clearFilesDir(context)
				
				settings?.also(File(context.filesDir, SETTINGS_FILE)::writeBytes)
				data?.also(File(context.filesDir, TASKS_LIST_FILE)::writeBytes)
				
				val iconCache = File(context.filesDir, ICON_CACHE_FOLDER)
				iconCache.mkdir()
				Log.w("BACKUP", "icons: ${icons.size}")
				icons.forEach { (name, raw) ->
					val file = File(iconCache,name)
					file.writeBytes(raw)
				}
				
				restartApp(context)
			}
			else {
				Log.w("BACKUP", "Invalid backup.")
				AlertDialog.Builder(context)
					.setTitle("Invalid backup!")
					.setMessage("The selected file is not a valid backup.")
					.setNeutralButton("Ok", null)
					.show()
			}
		}
		
		private fun clearFilesDir(context: Context) {
			context.filesDir.listFiles()?.forEach { child ->
				child.deleteRecursively()
			}
		}
		
		private fun restartApp(context: Context) {
			
			val launchIntent = Intent(context, MainActivity::class.java).apply {
				flags = Intent.FLAG_ACTIVITY_NEW_TASK or
						Intent.FLAG_ACTIVITY_CLEAR_TASK
			}
			
			val pendingIntent = PendingIntent.getActivity(
				context,
				RESTART_REQUEST_CODE,
				launchIntent,
				PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
			
			val alarmManager = context.getSystemService(AlarmManager::class.java)
			alarmManager.set(
			AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + 500,
				pendingIntent)
			
			Toast.makeText(context, "Restarting … please wait!", Toast.LENGTH_LONG).show()
			Process.killProcess(Process.myPid())
		}
	}
	
	fun restore(context: Context, uri: Uri) {
		context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
		context.contentResolver.openInputStream(uri)
			?.let { ZipInputStream(it)}
			?.use { zip ->
				val rawContents = RawContents()
				while (true) {
					val entry = zip.nextEntry ?: break
					when (entry.name) {
						ICON_CACHE_FOLDER -> rawContents.hasIcons = true
						TASKS_LIST_FILE -> rawContents.data = zip.readBytes()
						SETTINGS_FILE -> rawContents.settings = zip.readBytes()
						else -> if (entry.name.startsWith(ICON_CACHE_FOLDER)) {
							val file = File(entry.name)
							file.parent?.takeIf(ICON_CACHE_FOLDER::startsWith)
								?.also { rawContents.icons[file.name] = zip.readBytes() }
						}
					}
					zip.closeEntry()
				}
				rawContents.writeContent(context)
			}
	}
}