package kryptonbutterfly.checklist

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity.CENTER_VERTICAL
import android.view.View
import android.view.View.GONE
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.setMargins
import androidx.core.view.size
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kryptonbutterfly.checklist.Constants.MOVE_TASK
import kryptonbutterfly.checklist.Constants.MSG_DELETE_ALL_TASKS
import kryptonbutterfly.checklist.Constants.TASKS_INDEX
import kryptonbutterfly.checklist.Constants.TEXT_CANCEL
import kryptonbutterfly.checklist.Constants.TEXT_DELETE
import kryptonbutterfly.checklist.Constants.UNCATEGORIZED
import kryptonbutterfly.checklist.actions.Action
import kryptonbutterfly.checklist.actions.CreateTask
import kryptonbutterfly.checklist.actions.DeleteTask
import kryptonbutterfly.checklist.actions.MoveTask
import kryptonbutterfly.checklist.misc.dragHelper
import kryptonbutterfly.checklist.misc.setAnimatorDurations
import kryptonbutterfly.checklist.persistence.cache
import kryptonbutterfly.checklist.persistence.data
import kryptonbutterfly.checklist.ui.SimpleConfirmDialog
import kryptonbutterfly.checklist.ui.TaskAdapter
import kryptonbutterfly.checklist.ui.TaskVariants

class CompletedTasksActivity: AppCompatActivity(), HistoryActivity {
	private val rowOddColor = TypedValue()
	private val rowEvenColor = TypedValue()
	private lateinit var textListName: TextView
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_done_tasks)
		
		theme.resolveAttribute(R.attr.row_even_color, rowEvenColor, true)
		theme.resolveAttribute(R.attr.row_odd_color, rowOddColor, true)
		
		this.textListName = findViewById(R.id.textListName)
		
		setAnimatorDurations(findViewById<RecyclerView>(R.id.doneList).itemAnimator)
	}
	override fun onResume() {
		super.onResume()
		populateUI()
	}
	private fun populateUI() {
		Log.v("populateUI", "Populating UI")
		
		val data = data(this)
		val currList = data.currentList()
		
		textListName.text = data.currentList
		
		val unspecified = findViewById<RecyclerView>(R.id.doneList)
		unspecified.adapter = TaskAdapter(
			this,
			currList.done.getOrPut(UNCATEGORIZED, ::ArrayList),
			UNCATEGORIZED,
			data.currentList,
			TaskVariants.DONE
		)
		ItemTouchHelper(dragHelper).attachToRecyclerView(unspecified)
		
		findViewById<LinearLayout>(R.id.doneCategories).removeAllViews()
		currList.done.keys.forEach(this::getOrCreateCategory)
		
		updateUI()
	}
	private fun getOrCreateCategory(categoryId: Long): RecyclerView {
		if (categoryId == UNCATEGORIZED) return findViewById(R.id.doneList)
		
		val categories = findViewById<LinearLayout>(R.id.doneCategories)
		for (view in categories.children) if (categoryId == view.tag as Long) {
			val child = (view as LinearLayout).getChildAt(TASKS_INDEX) as RecyclerView
			return child
		}
		return createCategory(categories, categoryId)
	}
	private fun createCategory(categories: LinearLayout, categoryId: Long): RecyclerView {
		val data = data(this)
		val category = data.categories[categoryId]
		val icon = category?.icon.let { cache(this).iconMap[it] }
		
		val categoryView = LinearLayout(applicationContext)
		categories.addView(categoryView)
		val vertLayout = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
		categoryView.layoutParams = vertLayout
		categoryView.orientation = LinearLayout.VERTICAL
		categoryView.tag = categoryId
		
		val categoryTitle = LinearLayout(applicationContext)
		categoryView.addView(categoryTitle)
		categoryTitle.layoutParams = vertLayout
		
		val templateIcon = findViewById<ImageView>(R.id.categoryTemplateIcon)
		run {
			val catIcon = ImageView(applicationContext)
			categoryTitle.addView(catIcon)
			val width = templateIcon.layoutParams.width
			val height = templateIcon.layoutParams.height
			val catIconLayout = LinearLayout.LayoutParams(width, height)
			catIcon.layoutParams = catIconLayout
			catIcon.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
			icon?.also { catIcon.setImageBitmap(it) }
		}
		
		val templateText = findViewById<TextView>(R.id.categoryTemplateName)
		val catText = TextView(applicationContext)
		categoryTitle.addView(catText)
		val catTextLayout = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
		catText.layoutParams = catTextLayout
		catTextLayout.gravity = CENTER_VERTICAL
		catTextLayout.setMargins(12)
		catText.setTextColor(templateText.textColors)
		catText.setTextSize(TypedValue.COMPLEX_UNIT_SP, templateText.textSize)
		category?.name?.also { catText.text = it }
		
		val currList = data.currentList()
		
		val backingList = currList.done.getOrPut(categoryId, ::ArrayList)
		val adapter = TaskAdapter(this, backingList, categoryId, data.currentList, TaskVariants.DONE)
		
		val tasks = RecyclerView(applicationContext)
		val tasksLayout = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
		tasks.layoutParams = tasksLayout
		tasks.layoutManager = LinearLayoutManager(this)
		tasks.adapter = adapter
		tasks.isNestedScrollingEnabled = false
		
		setAnimatorDurations(tasks.itemAnimator)
		
		categoryView.addView(tasks)
		ItemTouchHelper(dragHelper).attachToRecyclerView(tasks)
		return tasks
	}
	private fun onDialogPositiveClick() {
		Log.i("DONE", "delete all done Tasks")
		
		val currList = data(this).currentList()
		currList.done.clear()
		val doneList = findViewById<RecyclerView>(R.id.doneList)
		(doneList.adapter as? TaskAdapter<*>)?.also {
			val count = it.itemCount
			it.tasks.clear()
			Handler(Looper.getMainLooper()).post{ it.notifyItemRangeRemoved(0, count)}
		}
		
		
		val categories = findViewById<LinearLayout>(R.id.doneCategories)
		categories.removeAllViews()
		currList.history.clear()
	}
	override fun setItemBG(target: View, pos: Int) {
		target.setBackgroundColor(if (pos % 2 == 1) rowOddColor.data else rowEvenColor.data)
	}
	private fun updateUI() {
		Log.i("updateUI", "updating UI")
		fun colorRows(tasks: RecyclerView) {
			for (i in 0 until tasks.size) {
				val child = tasks[i]
				val pos = tasks.getChildViewHolder(child).bindingAdapterPosition
				setItemBG(child, pos)
			}
		}
		
		val taskList = findViewById<RecyclerView>(R.id.doneList)
		colorRows(taskList)
		
		findViewById<LinearLayout>(R.id.doneCategories).forEach { category ->
			val tasks = (category as LinearLayout).getChildAt(TASKS_INDEX) as RecyclerView
			category.visibility = if ((tasks.adapter?.itemCount ?: 0) == 0) GONE else VISIBLE
			colorRows(tasks)
		}
	}
	override fun editTask(index: Int, categoryId: Long, description: String) {
		Log.i("EDIT_TASK", "Ignore edit done task.")
	}
	private fun markTodo(action: CreateTask) {
		Log.i("DoneTasksActivity", "markTodo $action")
		val currList = data(this).currentList()
		action.doneIndex?.also { doneIndex ->
			val done = getOrCreateCategory(action.category)
			(done.adapter as? TaskAdapter<*>)?.also {
				it.tasks.removeAt(doneIndex)
				it.notifyItemRemoved(doneIndex)
				val category = currList.tasks.getOrPut(action.category, ::ArrayList)
				val index = if (action.index != -1) action.index else category.size
				category.add(index, action.description)
				currList.history.clear()
			}
		}
	}
	private fun deleteTask(action: DeleteTask) {
		Log.i("DoneTasksActivity", "delete $action")
		val currList = data(this).currentList()
		action.doneIndex?.also { doneIndex ->
			val done = getOrCreateCategory(action.category)
			(done.adapter as? TaskAdapter<*>)?.also {
				it.tasks.removeAt(doneIndex)
				it.notifyItemRemoved(doneIndex)
				currList.history.clear()
			}
		}
	}
	private fun moveTask(action: MoveTask) {
		Log.i(MOVE_TASK, "Moving row ${action.old} to ${action.new}")
		
		val category = getOrCreateCategory(action.categoryID)
		
		val range = IntRange(0, category.size-1)
		if (action.old !in range) {
			Log.i(MOVE_TASK, "old: ${action.old} is out of range $range")
			return
		}
		if (action.new !in range) {
			Log.i(MOVE_TASK, "new: ${action.new} is out of range $range")
			return
		}
		(category.adapter as? TaskAdapter<*>)?.moveItem(action.old, action.new, false)
		data(this).currentList().history.clear()
	}
	override fun event(action: Action<*>) {
		when (action) {
			is CreateTask -> markTodo(action)
			is DeleteTask -> deleteTask(action)
			is MoveTask -> moveTask(action)
			else -> Log.w("COMPLETED_TASK_ACTIVITY", "unsupported action $action")
		}
	}
	private fun taskCount(): Int {
		return data(this).currentList().done.values.stream().mapToInt { tasks -> tasks.size }.sum()
	}
	fun deleteAllClicked(@Suppress("UNUSED_PARAMETER") view: View) {
		if (taskCount() > 0)
			SimpleConfirmDialog {
				AlertDialog.Builder(it).setMessage(MSG_DELETE_ALL_TASKS)
					.setPositiveButton(TEXT_DELETE) { _, _ -> onDialogPositiveClick() }
					.setNegativeButton(TEXT_CANCEL) { _, _ -> }
					.create()
			}.show(supportFragmentManager, "DeleteAllDialog")
	}
	fun backToTasks(@Suppress("UNUSED_PARAMETER") view: View) {
		Log.i("DONE", "back to Tasks")
		finish()
	}
}