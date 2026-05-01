package kryptonbutterfly.checklist

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity.CENTER_VERTICAL
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.core.view.size
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kryptonbutterfly.checklist.Constants.ACTION
import kryptonbutterfly.checklist.Constants.CREATE_TASK
import kryptonbutterfly.checklist.Constants.DESCRIPTION
import kryptonbutterfly.checklist.Constants.INDEX
import kryptonbutterfly.checklist.Constants.CATEGORY
import kryptonbutterfly.checklist.Constants.CATEGORY_HEADER_INDEX
import kryptonbutterfly.checklist.Constants.CATEGORY_TITLE_INDEX
import kryptonbutterfly.checklist.Constants.CHANGE_TASK
import kryptonbutterfly.checklist.Constants.MOVE_TASK
import kryptonbutterfly.checklist.Constants.TASKS_INDEX
import kryptonbutterfly.checklist.Constants.UNCATEGORIZED
import kryptonbutterfly.checklist.actions.*
import kryptonbutterfly.checklist.misc.Stack
import kryptonbutterfly.checklist.persistence.*
import kryptonbutterfly.checklist.ui.ItemTouchViewHolder
import kryptonbutterfly.checklist.ui.TaskAdapter

const val REQUEST_PERMISSION_CODE = 0
class MainActivity : AppCompatActivity(), DeleteAllDialog.DialogListener {
    private val rowOddColor = TypedValue()
    private val rowEvenColor = TypedValue()
    private lateinit var dropDown: CardView
    private lateinit var spinnerList: Spinner
    private lateinit var listsAdapter: ArrayAdapter<String>
    
    private val history = Stack<Action<*>>()
    
    private val dragHelper = object: ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0) {
        
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            (recyclerView.adapter as? TaskAdapter)?.triggerMove(from, to)
            return true
        }
        
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            //nothing to do here
        }
        
        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE)
                (viewHolder as? ItemTouchViewHolder)?.onItemSelected()
            super.onSelectedChanged(viewHolder, actionState)
        }
        
        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as? ItemTouchViewHolder)?.onItemClear()
        }
        
        override fun isItemViewSwipeEnabled() = false
        
        override fun isLongPressDragEnabled() = true
    }
    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                (result.data?.getSerializableExtra(ACTION) as? Action<*>)
                    ?.also(this::event)
        }

    private val getExport =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                result.data?.data?.also(this::exportData)
        }
    
    private val getImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri->
            uri?.also(this::importData)
        }

    private val settingsResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                history.limit = settings(this).undoLength
        }
    
    private val editResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                (result.data?.getSerializableExtra(CATEGORY) as? Category)?.also { cat ->
                    data(this).categories[cat.id] = cat
                    val categories = findViewById<LinearLayout>(R.id.categories)
                    (((categories.children.firstOrNull { cat.id == it.tag } as? LinearLayout)?.
                        getChildAt(CATEGORY_HEADER_INDEX) as? LinearLayout)?.
                        getChildAt(CATEGORY_TITLE_INDEX) as? TextView)?.text = cat.name
                }
        }
    
    private val addListResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                populateUI()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        theme.resolveAttribute(R.attr.row_even_color, rowEvenColor, true)
        theme.resolveAttribute(R.attr.row_odd_color, rowOddColor, true)
        
        this.dropDown = findViewById(R.id.dropdown)
        this.spinnerList = findViewById(R.id.spinnerList)
        
        this.listsAdapter = ArrayAdapter(this, android.R.layout.simple_selectable_list_item, ArrayList())
        spinnerList.adapter = this.listsAdapter
        spinnerList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, index: Int, p3: Long ) {
                listsAdapter.getItem(index)?. also {
                    val data = data(this@MainActivity)
                    if (data.currentList != it) {
                        data.currentList = it
                        populateUI()
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (this::dropDown.isInitialized && this.dropDown.isVisible) {
            val loc = IntArray(2)
            this.dropDown.getLocationOnScreen(loc)
            val rect = Rect(loc[0], loc[1], loc[0] + dropDown.width, loc[1] + dropDown.height)
            val x = ev.rawX.toInt()
            val y = ev.rawY.toInt()
            if (!rect.contains(x, y)) {
                if (ev.action == MotionEvent.ACTION_UP) {
                    this.dropDown.visibility = GONE
                    return true
                } else {
                    return false
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        populateUI()
    }
    
    private fun populateUI() {
        Log.v("populateUI", "Populating UI")
        history.limit = settings(this).undoLength
        
        val data = data(this)
        val currList = data.currentList()
        
        listsAdapter.clear()
        listsAdapter.addAll(data.lists.keys)
        spinnerList.setSelection(listsAdapter.getPosition(data.currentList))
        
        val unspecified = findViewById<RecyclerView>(R.id.taskList)
        unspecified.adapter = TaskAdapter(
            this,
            currList.tasks.getOrPut(UNCATEGORIZED, ::ArrayList),
            UNCATEGORIZED,
            data.currentList)
        ItemTouchHelper(dragHelper).attachToRecyclerView(unspecified)
        
        findViewById<LinearLayout>(R.id.categories).removeAllViews()
        currList.tasks.keys.forEach(this::getOrCreateCategory)
        
        updateUI()
    }
    
    override fun onPause() {
        super.onPause()
    
        saveSettings(this)
        val data = data(this)
        data.currentList().history = this.history.backingList
        saveData(this)
    }

    fun onExportClick(@Suppress("UNUSED_PARAMETER") view: View) {
        dropDown.visibility = GONE
        openFilePicker()
    }
    
    fun onImportClick(@Suppress("UNUSED_PARAMETER") view: View) {
        dropDown.visibility = GONE
        getImport.launch(arrayOf("text/markdown"))
    }


    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") view: View) {
        dropDown.visibility = GONE
        val intent = Intent(this, SettingsActivity::class.java)
        settingsResult.launch(intent)
    }
    
    fun onAddListClick(@Suppress("UNUSED_PARAMETER") view: View) {
        dropDown.visibility = GONE
        addListResult.launch(Intent(this, EditListActivity::class.java))
        
    }
    
    fun onDropDownClick(@Suppress("UNUSED_PARAMETER") view: View) {
        dropDown.visibility = VISIBLE
    }

    fun onAddClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, CreateTaskActivity::class.java)
        getContent.launch(intent)
    }

    fun onDeleteAllClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val taskCount = taskCount()
        if (taskCount > 0)
            DeleteAllDialog().show(supportFragmentManager, "DeleteAllDialog")
    }

    private fun taskCount(): Int {
        return data(this).currentList().tasks.values.stream()
            .mapToInt { tasks -> tasks.size }
            .sum()
    }
    
    override fun onDialogPositiveClick() {
        val taskCount = taskCount()
        
        event(DeleteAll(taskCount))
        data(this).currentList().tasks.clear()
        findViewById<RecyclerView>(R.id.taskList).adapter?.also { it.notifyDataSetChanged() }
        
        val categories = findViewById<LinearLayout>(R.id.categories)
        categories.removeAllViews()
        
    }

    fun onRestoreClick(@Suppress("UNUSED_PARAMETER") view: View) {
        history.remove()?.also(::undo)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    openFilePicker()
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/markdown"
        intent.putExtra(Intent.EXTRA_TITLE, "${data(this).currentList}.md")
        getExport.launch(intent)
    }

    private fun importData(uri: Uri) {
        val settings = settings(this)
        import(this, uri)?.also { importData ->
            val data = data(this)
            val targetList = data.lists.getOrPut(importData.listName ?: data.currentList, ::CheckList)
            
            fun skipTask(catId: Long, iTask: String): Boolean {
                return settings.skipExistingTasks &&
                        targetList.tasks[catId]?.contains(iTask)?:false
            }
            
            importData.tasks.forEach { catName, iTasks ->
                data.categories.values.firstOrNull { it.name == catName }?.also { cat ->
                    iTasks.filter { taskName -> !skipTask(cat.id, taskName) }
                        .forEach { targetList.addTask(cat.id, it, -1) }
                } ?: run {
                        val cat = Category(data, catName, null)
                        val catId: Long
                        if (catName.isNotBlank()) {
                            data.categories.put(cat.id, cat)
                            catId = cat.id
                        } else
                            catId = UNCATEGORIZED
                        iTasks.filter { taskName -> !skipTask(catId, taskName) }
                            .forEach { targetList.addTask(catId, it, -1) }
                }
            }
            if (importData.tasks.isNotEmpty()) {
                targetList.history.clear()
                history.clear()
            }
            
            populateUI()
        }
    }
    
    private fun exportData(uri: Uri) {
        val data = data(this)
        val sb = StringBuilder()
        
        fun printTask(tasks: ArrayList<String>) {
            tasks.forEach { sb.append(" * ${it}\n")}
        }
        
        sb.append("# ${data.currentList}\n")
        
        val list = data.currentList()
        list.tasks[UNCATEGORIZED]?.also(::printTask)
        
        list.tasks.forEach { catId, tasks ->
            if (tasks.isNotEmpty())
                data.categories[catId]?.also { category ->
                    sb.append("## ${category.name}\n")
                    printTask(tasks)
            }
        }
        
        val result = sb.toString()
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { br ->
            Log.i("EXPORTING", "File: $uri\tdata:\n$result")
            br.write(result)
        }
    }
    
    private fun changeTask(action: ChangeTask) {
        val tasksNew = getOrCreateCategory(action.catNew)
        if (action.catNew == action.catOld) {
            if (action.indOld != action.indNew) {
                Log.e(CHANGE_TASK, "Expected indNew (${action.indNew}) to == indOld (${action.indOld})")
                return
            }
            (tasksNew.adapter as? TaskAdapter)?.also {
                it.tasks[action.indNew] = action.descNew
                it.notifyItemChanged(action.indNew)
            }
            
        } else {
            val tasksOld = getOrCreateCategory(action.catOld)
            if (action.indOld !in IntRange(0, tasksOld.childCount)) {
                Log.e(CHANGE_TASK, "indOld (${action.indOld} not in range [0, ${tasksOld.childCount})")
                return
            }
            if (action.indNew != -1 && action.indNew !in IntRange(0, tasksNew.childCount + 1)) {
                Log.e(CHANGE_TASK, "Expected indNew to be -1 or be in range [0, ${tasksNew.childCount + 1}) but was ${action.indNew}")
                return
            }
            deleteTask(DeleteTask(action.descOld, action.listName,action.catOld, action.indOld))
            createTask(CreateTask(action.descNew, action.listName,action.catNew, action.indNew))
        }
    }
    
    private fun getOrCreateCategory(categoryId: Long) : RecyclerView {
        if (categoryId == UNCATEGORIZED)
            return findViewById(R.id.taskList)
        
        val categories = findViewById<LinearLayout>(R.id.categories)
        for (view in categories.children)
            if (categoryId == view.tag as Long) {
                val child = (view as LinearLayout).getChildAt(TASKS_INDEX) as RecyclerView
                return child
            }
        return createCategory(categories, categoryId)
    }
    
    private fun createCategory(categories: LinearLayout, categoryId: Long): RecyclerView {
        val data = data(this)
        val category = data.categories[categoryId]
        val icon = category?.icon?.let { cache(this).iconMap[it] }
        
        val categoryView = LinearLayout(applicationContext)
        categories.addView(categoryView)
        val vertLayout = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        categoryView.layoutParams = vertLayout
        categoryView.orientation = LinearLayout.VERTICAL
        categoryView.tag = categoryId
        
        val categoryTitle = LinearLayout(applicationContext)
        categoryView.addView(categoryTitle)
        categoryTitle.layoutParams = vertLayout
        categoryTitle.setOnClickListener { view ->
            val intent = Intent(this, EditCategory::class.java)
            intent.putExtra(CATEGORY, categoryId)
            editResult.launch(intent)
        }
        
        val templateIcon = findViewById<ImageView>(R.id.categoryTemplateIcon)
        run {
            val catIcon = ImageView(applicationContext)
            categoryTitle.addView(catIcon)
            val width = templateIcon.layoutParams.width
            val height = templateIcon.layoutParams.height
            val catIconLayout = LinearLayout.LayoutParams(width, height)
            catIcon.layoutParams = catIconLayout
            catIcon.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            icon?.also {
                catIcon.setImageBitmap(it)
            }
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
        
        val backingList = currList.tasks.getOrPut(categoryId, ::ArrayList)
        val adapter = TaskAdapter(this, backingList, categoryId, data.currentList)
        
        val tasks = RecyclerView(applicationContext)
        val tasksLayout = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        tasks.layoutParams = tasksLayout
        tasks.layoutManager = LinearLayoutManager(this)
        tasks.adapter = adapter
        tasks.isNestedScrollingEnabled = false
        
        categoryView.addView(tasks)
        ItemTouchHelper(dragHelper).attachToRecyclerView(tasks)
        return tasks
    }
    
    private fun createTask(action: CreateTask) {
        val tasks = getOrCreateCategory(action.category)
        Log.i(CREATE_TASK, "Adding task @ ${action.index}")
        
        val i = if (action.index == -1) tasks.size else action.index.coerceIn(0, tasks.size)
        
        (tasks.adapter as? TaskAdapter)?.also {
            it.tasks.add(i, action.description)
            it.notifyItemInserted(i)
        }?:run {
            Log.w(CREATE_TASK, "Failed to retrieve category from backing data.")
        }
    }

    fun editTask(index: Int, categoryId: Long, description: String) {
        val intent = Intent(this, CreateTaskActivity::class.java)
        intent.putExtra(CATEGORY, categoryId)
        intent.putExtra(INDEX, index)
        intent.putExtra(DESCRIPTION, description)
        getContent.launch(intent)
    }

    private fun deleteTask(action: DeleteTask) {
        val tasks = getOrCreateCategory(action.category)
        (tasks.adapter as? TaskAdapter)?.also {
            it.tasks.removeAt(action.index)
            it.notifyItemRemoved(action.index)
        }
    }

    private fun moveTask(action: MoveTask) {
        Log.i(MOVE_TASK, "Moving row ${action.old} to ${action.new}")
        
        val category = getOrCreateCategory(action.categoryID)
        
        val range = IntRange(0, category.size - 1)
        if (action.old !in range) {
            Log.i(MOVE_TASK, "old: ${action.old} is out of range $range")
            return
        }
        if (action.new !in range) {
            Log.i(MOVE_TASK, "new: ${action.new} is out of range $range")
            return
        }
        (category.adapter as? TaskAdapter)?.moveItem(action.old, action.new, false)
    }
    
    fun event(action: Action<*>) {
        val settings = settings(this)
        when (action) {
            is CreateTask -> {
                 createTask(action)
                if (settings.trackCreate)
                    history.add(action)
                else
                    history.clear()
            }
            is ChangeTask -> {
                changeTask(action)
                if (settings.trackRename)
                    history.add(action)
                else
                    history.clear()
            }
            is MoveTask -> {
                moveTask(action)
                if (settings.trackMove)
                    history.add(action)
                else
                    history.remove()
            }
            is DeleteTask -> {
                deleteTask(action)
                if (settings.trackDelete)
                    history.add(action)
                else
                    history.clear()
            }
            is DeleteAll -> history.clear()
        }
        updateUI()
    }

    private fun undo(action: Action<*>) {
        when (action) {
            is CreateTask -> deleteTask(action.inverse())
            is ChangeTask -> changeTask(action.inverse())
            is MoveTask -> moveTask(action.inverse())
            is DeleteTask -> createTask(action.inverse())
            is DeleteAll -> {}
        }
        updateUI()
    }

    fun setItemBG(target: View, pos: Int) {
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
        
        val taskList = findViewById<RecyclerView>(R.id.taskList)
        colorRows(taskList)
        
        findViewById<LinearLayout>(R.id.categories).forEach { category ->
            val tasks = (category as LinearLayout).getChildAt(TASKS_INDEX) as RecyclerView
            category.visibility = if ((tasks.adapter?.itemCount?:0) == 0) GONE else VISIBLE
            colorRows(tasks)
        }
        
        findViewById<ImageButton>(R.id.restoreButton).visibility =
            if (history.isEmpty()) GONE else VISIBLE
    }
}
