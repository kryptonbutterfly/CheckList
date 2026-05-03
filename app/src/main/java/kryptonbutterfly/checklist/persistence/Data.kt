package kryptonbutterfly.checklist.persistence

import android.content.Context
import android.util.Log
import com.google.gson.annotations.Expose
import kryptonbutterfly.checklist.Constants.GSON
import kryptonbutterfly.checklist.Constants.UNCATEGORIZED
import java.io.File
import java.io.Serializable
import java.util.*

private const val TASKS_LIST_FILE: String = "TasksListData.json"

private fun loadData(context: Context): Data {
    val file = File(context.filesDir, TASKS_LIST_FILE)
    if (!file.exists())
        return Data()
    val json = file.bufferedReader().use { it.readText() }
    return GSON.fromJson(json, Data::class.java)
}

fun saveData(context: Context) {
    data?.also {data ->
        data.pruneLists()
        val json = GSON.toJson(data)
        val file = File(context.filesDir, TASKS_LIST_FILE)
        if (file.parentFile != null && !file.parentFile!!.exists())
            file.parentFile!!.mkdirs()
        file.bufferedWriter().use { it.write(json) }
    }
}

private var data : Data? = null
fun data(context: Context): Data {
    return data ?: loadData(context).also { data = it }
}

data class Data(
    @Expose private var categoryIdSource: Long = UNCATEGORIZED + 1,
    @Expose var currentList: String = "Default",
    @Expose val lists: HashMap<String, CheckList> = HashMap(),
    @Expose val categories: HashMap<Long, Category> = HashMap()) : Serializable {
    
    fun genCategoryID(): Long {
        return categoryIdSource++
    }
    
    fun currentList() : CheckList {
        return lists.getOrPut(currentList, ::CheckList)
    }
    
    fun prune() {
        Log.i("DATA", "pruning")
     
        val unusedCategories = HashSet(categories.keys)
        lists.values.forEach { it.removeUsedCategoriesFromSet(unusedCategories) }
        unusedCategories.forEach { it -> categories.remove(it) }
    }
    fun pruneLists() {
        Log.i("DATA", "pruning lists")
        
        lists.values.forEach { it.pruneCategories() }
        
        lists.entries.removeIf { it.value.tasks.isEmpty() && it.key != currentList }
    }
}
