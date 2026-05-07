package kryptonbutterfly.checklist.persistence

import android.content.ContextWrapper
import android.util.Log
import com.google.gson.annotations.Expose
import kryptonbutterfly.checklist.Constants.GSON
import java.io.File
import java.io.Serializable

private const val SETTINGS_FILE: String = "Settings.json"

private fun loadSettings(context: ContextWrapper):Settings {
    val file = File(context.filesDir, SETTINGS_FILE)
    if (!file.exists())
        return Settings()
    val json = file.bufferedReader().use { it.readText() }
    val data = GSON.fromJson(json, Settings::class.java)
    raw = json
    return data
}

fun saveSettings(context: ContextWrapper) {
    val json = GSON.toJson(settings)
    val file = File(context.filesDir, SETTINGS_FILE)
    if (file.exists() && json.equals(raw)) {
        Log.d("PERSIST-SETTINGS", "skip persisting — file exists and nothing has changed!")
        return
    }
    
    if (file.parentFile != null && !file.parentFile!!.exists())
        file.parentFile!!.mkdirs()
    
    file.bufferedWriter().use { it.write(json) }
    raw = json
}

private var raw: String? = null
private var settings : Settings? = null
fun settings(context: ContextWrapper): Settings {
    return settings ?: loadSettings(context).also { settings = it }
}

data class Settings(
    @Expose var undoLength: Int = 16,
    @Expose var trackCreate: Boolean = false,
    @Expose var trackRename: Boolean = true,
    @Expose var trackMove: Boolean = false,
    @Expose var trackDelete: Boolean = true,
    @Expose var skipExistingTasks: Boolean = false): Serializable