package kryptonbutterfly.checklist.persistence

import android.content.Context
import android.net.Uri

data class Import(
	var listName: String? = null,
	val tasks: HashMap<String, ArrayList<String>> = HashMap()) {
	var currentCategory: String = ""
	
	fun addTask(task: String) {
		tasks.getOrPut(currentCategory, ::ArrayList).add(task)
	}
}

private val ENTRY_START: Regex = Regex("\n\\s*(##|\\*)")

fun import(context: Context, uri: Uri): Import? {
	context.contentResolver.openInputStream(uri)?.use { iStream ->
		iStream.reader(Charsets.UTF_8).use {
			var lines = it.readText()
			
			fun nextEntry(): String {
				ENTRY_START.find(lines)?.range?.first?.let { start ->
					val entry = lines.substring(0, start)
					lines = lines.substring(start+1).trim()
					return entry
				}
				val entry = lines
				lines = ""
				return entry
			}
			
			val import: Import =
			if (lines.startsWith("#") && !lines.startsWith("##")) {
				val entry = nextEntry()
				Import(entry.substring(entry.indexOf("#") + 1).trim())
			} else
				Import()
			
			while (lines.isNotBlank()) {
				val entry = nextEntry()
				if (entry.startsWith("##"))
					import.currentCategory = entry.substring(2).trim()
				else
					import.addTask(entry.substring(1).trim())
			}
			return import
		}
	}
	return null
}