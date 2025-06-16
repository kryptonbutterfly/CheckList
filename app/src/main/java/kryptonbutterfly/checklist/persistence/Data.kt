package kryptonbutterfly.checklist.persistence

import android.content.ContextWrapper
import com.google.gson.annotations.Expose
import kryptonbutterfly.checklist.Constants.GSON
import kryptonbutterfly.checklist.actions.Action
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

private const val TASKS_LIST_FILE: String = "TasksListData.json"

fun loadData(context: ContextWrapper): Data {
    val file = File(context.filesDir, TASKS_LIST_FILE)
    if (!file.exists())
        return Data()
    val json = file.bufferedReader().use { it.readText() }
    return GSON.fromJson(json, Data::class.java)
}

fun saveData(context: ContextWrapper, data: Data) {
    val json = GSON.toJson(data)
    val file = File(context.filesDir, TASKS_LIST_FILE)
    if (file.parentFile != null && !file.parentFile!!.exists())
        file.parentFile!!.mkdirs()
    file.bufferedWriter().use { it.write(json) }
}

data class Data(@Expose var tasks: ArrayList<String> = ArrayList(), @Expose var history: LinkedList<Action<*>> = LinkedList())
