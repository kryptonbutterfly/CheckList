package kryptonbutterfly.checklist.persistence

import android.content.ContextWrapper
import android.util.Log
import com.google.gson.annotations.Expose
import kryptonbutterfly.checklist.Constants.GSON
import kryptonbutterfly.checklist.Constants.UNCATEGORIZED
import kryptonbutterfly.checklist.actions.Action
import kryptonbutterfly.checklist.actions.ChangeTask
import kryptonbutterfly.checklist.actions.CreateTask
import kryptonbutterfly.checklist.actions.DeleteAll
import kryptonbutterfly.checklist.actions.DeleteTask
import kryptonbutterfly.checklist.actions.MoveTask
import java.io.File
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

private const val TASKS_LIST_FILE: String = "TasksListData.json"

private fun loadData(context: ContextWrapper): Data {
    val file = File(context.filesDir, TASKS_LIST_FILE)
    if (!file.exists())
        return Data()
    val json = file.bufferedReader().use { it.readText() }
    return GSON.fromJson(json, Data::class.java)
}

fun saveData(context: ContextWrapper) {
    val json = GSON.toJson(data)
    val file = File(context.filesDir, TASKS_LIST_FILE)
    if (file.parentFile != null && !file.parentFile!!.exists())
        file.parentFile!!.mkdirs()
    file.bufferedWriter().use { it.write(json) }
}

private var data : Data? = null
fun data(context: ContextWrapper): Data {
    return data ?: loadData(context).also { data = it }
}

data class Data(
    @Expose private var categoryIdSource: Long = UNCATEGORIZED + 1,
    @Expose val tasks: HashMap<Long, ArrayList<String>> = HashMap(),
    @Expose val categories: HashMap<Long, Category> = HashMap(),
    @Expose var history: LinkedList<Action<*>> = LinkedList()) : Serializable {
    
    fun genCategoryID(): Long {
        return categoryIdSource++
    }
    
    fun addTask(categoryId: Long, description: String, index: Int) {
        val category = tasks[categoryId]
        if (category != null)
            if (index == -1)
                category.add(description)
            else
                category.add(index, description)
        else {
            val list = ArrayList<String>()
            list.add(description)
            tasks.put(categoryId, list)
        }
    }
    
    fun prune() {
        Log.i("DATA", "pruning")
        
        tasks.values.removeIf { catTasks -> catTasks.isEmpty() }
        
        val unused = HashSet(categories.keys)
        tasks.entries.forEach {
            if (it.value.isNotEmpty())
                unused.remove(it.key)
        }
        history.forEach { action ->
            when(action) {
                is CreateTask -> unused.remove(action.category)
                is ChangeTask -> {
                    unused.remove(action.catNew)
                    unused.remove(action.catOld)
                }
                is MoveTask -> unused.remove(action.categoryID)
                is DeleteTask -> unused.remove(action.category)
                is DeleteAll -> {}
            }
        }
        unused.forEach { it -> categories.remove(it) }
    }
}
