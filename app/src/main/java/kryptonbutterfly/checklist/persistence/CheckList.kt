package kryptonbutterfly.checklist.persistence

import com.google.gson.annotations.Expose
import kryptonbutterfly.checklist.Constants.UNCATEGORIZED
import kryptonbutterfly.checklist.actions.Action
import kryptonbutterfly.checklist.actions.ChangeTask
import kryptonbutterfly.checklist.actions.CreateTask
import kryptonbutterfly.checklist.actions.DeleteAll
import kryptonbutterfly.checklist.actions.DeleteTask
import kryptonbutterfly.checklist.actions.MoveTask
import java.io.Serializable
import java.util.LinkedList

data class CheckList(
	@Expose val tasks: HashMap<Long, ArrayList<String>> = HashMap(),
	@Expose var history: LinkedList<Action<*>> = LinkedList()
) : Serializable {
	fun addTask(categoryId: Long, descriptor: String, index: Int) {
		val categoryTasks = tasks[categoryId]
		if (categoryTasks != null)
			if (index == -1)
				categoryTasks.add(descriptor)
			else
				categoryTasks.add(index, descriptor)
		else {
			val list = ArrayList<String>()
			list.add(descriptor)
			tasks.put(categoryId, list)
		}
	}
	
	/**
	 * prune empty categories from this list
	 */
	fun pruneCategories() {
		tasks.entries.removeIf {
			it.value.isEmpty() && it.key != UNCATEGORIZED
		}
	}
	
	/**
	 * Expects a set of categoryIDs and removes any used in this list.
	 * @param unusedCategories A set of categoryIDs that have not yet been established as being in use
	 */
	fun removeUsedCategoriesFromSet(unusedCategories: HashSet<Long>) {
		tasks.entries.forEach {
			if (it.value.isNotEmpty())
				unusedCategories.remove(it.key)
		}
		
		history.forEach { action ->
			when(action) {
				is CreateTask -> unusedCategories.remove(action.category)
				is ChangeTask -> {
					unusedCategories.remove(action.catNew)
					unusedCategories.remove(action.catOld)
				}
				is MoveTask -> unusedCategories.remove(action.categoryID)
				is DeleteTask -> unusedCategories.remove(action.category)
				is DeleteAll -> {}
			}
		}
	}
}
