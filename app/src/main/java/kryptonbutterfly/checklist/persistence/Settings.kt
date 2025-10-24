package kryptonbutterfly.checklist.persistence

import android.content.ContextWrapper
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
    return GSON.fromJson(json, Settings::class.java)
}

fun saveSettings(context: ContextWrapper) {
    val json = GSON.toJson(settings)
    val file = File(context.filesDir, SETTINGS_FILE)
    if (file.parentFile != null && !file.parentFile!!.exists())
        file.parentFile!!.mkdirs()
    file.bufferedWriter().use { it.write(json) }
}

private var settings : Settings? = null
fun settings(context: ContextWrapper): Settings {
    return settings ?: loadSettings(context).also { settings = it }
}

data class Settings(
    @Expose var undoLength: Int = 16,
    @Expose var trackCreate: Boolean = false,
    @Expose var trackRename: Boolean = true,
    @Expose var trackMove: Boolean = false,
    @Expose var trackDelete: Boolean = true): Serializable