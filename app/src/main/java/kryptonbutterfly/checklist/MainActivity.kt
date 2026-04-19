package kryptonbutterfly.checklist

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
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
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.core.view.size
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
import kryptonbutterfly.checklist.Constants.TEXT_COLUMN
import kryptonbutterfly.checklist.Constants.UNCATEGORIZED
import kryptonbutterfly.checklist.actions.*
import kryptonbutterfly.checklist.misc.Stack
import kryptonbutterfly.checklist.misc.swap
import kryptonbutterfly.checklist.persistence.*
import kotlin.sequences.filter

const val REQUEST_PERMISSION_CODE = 0
class MainActivity : AppCompatActivity(), DeleteAllDialog.DialogListener {
    private val rowOddColor = TypedValue()
    private val rowEvenColor = TypedValue()

    private lateinit var dropDown: CardView
    private lateinit var spinnerList: Spinner
    private lateinit var listsAdapter: ArrayAdapter<String>
    
    private val roundedCorners = Drawable.createFromPath("@drawable/rounded_corner")
    private val history = Stack<Action<*>>()
    
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
                clearUI()
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
                        clearUI()
                        populateUI()
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (this::dropDown.isInitialized && this.dropDown.isVisible)
            ev?.takeIf { it.action == MotionEvent.ACTION_UP } ?.also {
                val loc = IntArray(2)
                dropDown.getLocationOnScreen(loc)
                val rect =
                    Rect(loc[0], loc[1], loc[0] + dropDown.width, loc[1] + dropDown.height)
                val x = it.rawX.toInt()
                val y = it.rawY.toInt()
                if (!rect.contains(x, y)) {
                    this.dropDown.visibility = GONE
                    return true
                }
            }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        populateUI()
    }
    
    private fun populateUI() {
        if (findViewById<TableLayout>(R.id.taskList).size != 0)
            return
        if (findViewById<LinearLayout>(R.id.categories).size != 0)
            return
        
        history.limit = settings(this).undoLength
        
        val data = data(this)
        
        val currList = data.currentList()
        listsAdapter.clear()
        listsAdapter.addAll(data.lists.keys)
        val index = listsAdapter.getPosition(data.currentList)
        spinnerList.setSelection(index)
        
        currList.tasks.forEach { cat, tasks -> this.createTasks(cat, tasks, data) }
        history.backingList = data.currentList().history
        
        updateUI()
    }
    
    private fun clearUI() {
        findViewById<TableLayout>(R.id.taskList).removeAllViews()
        findViewById<LinearLayout>(R.id.categories).removeAllViews()
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
        
        val taskList = findViewById<TableLayout>(R.id.taskList)
        taskList.removeAllViews()
        
        val categories = findViewById<LinearLayout>(R.id.categories)
        categories.removeAllViews()
        
        event(DeleteAll(taskCount))
        data(this).currentList().tasks.clear()
    }

    fun onRestoreClick(@Suppress("UNUSED_PARAMETER") view: View) {
        history.remove()?.also(::redo)
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
            
            clearUI()
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
            ((tasksNew[action.indNew] as TableRow)[TEXT_COLUMN] as TextView).text = action.descNew
            data(this).currentList().tasks[action.catNew]?.
                also { tasks -> tasks[action.indNew] = action.descNew }
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
    
    private fun getOrCreateCategory(categoryId: Long) : TableLayout {
        if (categoryId == UNCATEGORIZED)
            return findViewById(R.id.taskList)
        
        val categories = findViewById<LinearLayout>(R.id.categories)
        return categories.children.filter { view ->
            val id = view.tag as Long
            categoryId == id
        }.map { view ->
            (view as LinearLayout).getChildAt(TASKS_INDEX) as TableLayout
        }.firstOrNull() ?: createCategory(categories, categoryId)
    }
    
    private fun createCategory(categories: LinearLayout, categoryId: Long): TableLayout {
        val data = data(this)
        val category = data.categories[categoryId]
        val icon = category?.icon?.let { cache(this).iconMap[it] }
        
        val categoryView = LinearLayout(applicationContext)
        categories.addView(categoryView)
        val vertLayout = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        categoryView.layoutParams = vertLayout
        categoryView.orientation = LinearLayout.VERTICAL
        categoryView.tag = categoryId
        categoryView.setOnClickListener { view ->
            val intent = Intent(this, EditCategory::class.java)
            intent.putExtra(CATEGORY, categoryId)
            editResult.launch(intent)
        }
        
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
        
        val tasks = TableLayout(applicationContext)
        categoryView.addView(tasks)
        val tasksLayout = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        tasks.layoutParams = tasksLayout
        return tasks
    }
    
    private fun createTasks(categoryId: Long, tasks: ArrayList<String>, data: Data) {
        val category = data.categories[categoryId]
        if (category != null)
            Log.i(CREATE_TASK, "Creating Category '${category.name}' and it's tasks.")
        else
            Log.i(CREATE_TASK, "Creating uncategorized tasks.")
        tasks.forEach { description -> createTask(CreateTask(description, data.currentList, categoryId,-1), true) }
    }
    
    private fun createTask(action: CreateTask, isInit : Boolean = false) {
        val tasks = getOrCreateCategory(action.category)
        Log.i(CREATE_TASK, "Adding task @ ${action.index}")
        
        if (!isInit)
            data(this).addTask(action.listName,action.category, action.description, action.index)
        
        val i = if (action.index == -1) tasks.size else action.index.coerceIn(0, tasks.size)

        val taskDescTmpl = findViewById<TextView>(R.id.taskDescriptionTextTemplate)

        val row = TableRow(applicationContext)
        tasks.addView(row, i)
        row.layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        row.setPadding(16, 0, 16, 0)

        val textView = TextView(applicationContext)
        row.addView(textView)
        textView.layoutParams = taskDescTmpl.layoutParams
        textView.text = action.description
        textView.setTextColor(taskDescTmpl.textColors)
        textView.setPadding(0,8,0,8)
        textView.setOnClickListener {
            editTask(tasks.indexOfChild(row), action.category, textView.text.toString())
        }

        val deleteButton = ImageButton(applicationContext)
        row.addView(deleteButton)
        val deleteLayout = TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        deleteLayout.gravity = CENTER_VERTICAL
        deleteButton.layoutParams = deleteLayout
        deleteButton.setPadding(6, 6, 6, 6)
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete)
        deleteButton.background = roundedCorners
        deleteButton.setOnClickListener {
            event(DeleteTask(textView.text.toString(), action.listName, action.category,tasks.indexOfChild(row)))
        }

        val moveView = LinearLayout(applicationContext)
        row.addView(moveView)
        moveView.orientation = LinearLayout.VERTICAL
        moveView.layoutParams = TableRow.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
        moveView.weightSum = 0f

        val buttonUp = ImageButton(applicationContext)
        moveView.addView(buttonUp)
        buttonUp.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT, 0.5f)
        buttonUp.setImageResource(android.R.drawable.arrow_up_float)
        buttonUp.background = roundedCorners
        buttonUp.setOnClickListener { moveUp(tasks, row, action.category) }

        val buttonDown = ImageButton(applicationContext)
        moveView.addView(buttonDown)
        buttonDown.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT, 0.5f)
        buttonDown.setImageResource(android.R.drawable.arrow_down_float)
        buttonDown.background = roundedCorners
        buttonDown.setOnClickListener { moveDown(tasks, row, action.category) }
    }

    private fun editTask(index: Int, categoryId: Long, description: String) {
        val intent = Intent(this, CreateTaskActivity::class.java)
        intent.putExtra(CATEGORY, categoryId)
        intent.putExtra(INDEX, index)
        intent.putExtra(DESCRIPTION, description)
        getContent.launch(intent)
    }

    private fun moveUp(taskList: TableLayout, row: TableRow, categoryId: Long) {
        val index = taskList.indexOfChild(row)
        if (index <= 0)
            return

        val new = index - 1
        val previous = taskList[new] as TableRow
        val prevText = previous[TEXT_COLUMN] as TextView
        val data = data(this)
        data.currentList().tasks[categoryId]?.swap(new, index)
        event(MoveTask(index, new, prevText.text.toString(), categoryId, data.currentList))
    }

    private fun moveDown(taskList: TableLayout, row: TableRow, categoryId: Long) {
        val index = taskList.indexOfChild(row)
        val new = index + 1
        if (index >= taskList.size - 1)
            return

        val next = taskList[new] as TableRow
        val nextText = next[TEXT_COLUMN] as TextView
        event(MoveTask(index, new, nextText.text.toString(), categoryId, data(this).currentList))
    }

    private fun deleteTask(action: DeleteTask) {
        data(this).currentList().tasks[action.category]?.removeAt(action.index)
        getOrCreateCategory(action.category)
            .removeViewAt(action.index)
    }

    private fun moveTask(action: MoveTask) {
        Log.i(MOVE_TASK, "Moving row ${action.old} to ${action.new}")
        
        val tasks = getOrCreateCategory(action.categoryID)
        
        val range = IntRange(0, tasks.size - 1)
        if (action.old !in range) {
            Log.i(MOVE_TASK, "old: ${action.old} is out of range $range")
            return
        }
        if (action.new !in range) {
            Log.i(MOVE_TASK, "new: ${action.new} is out of range $range")
            return
        }

        val curr = tasks[action.old] as TableRow
        val next = tasks[action.new] as TableRow
        val currText = curr[TEXT_COLUMN] as TextView
        val nextText = next[TEXT_COLUMN] as TextView
        val swap = nextText.text
        nextText.text = currText.text
        currText.text = swap
    }
    
    private fun event(action: Action<*>) {
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

    private fun redo(action: Action<*>) {
        when (action) {
            is CreateTask -> deleteTask(action.inverse())
            is ChangeTask -> changeTask(action.inverse())
            is MoveTask -> moveTask(action.inverse())
            is DeleteTask -> createTask(action.inverse())
            is DeleteAll -> {}
        }
        updateUI()
    }

    private fun updateUI() {
        fun colorRows(tasks: TableLayout) {
            for (i in 0 until tasks.size)
                (tasks[i] as TableRow).setBackgroundColor((if (i % 2 == 1) rowOddColor else rowEvenColor).data)
        }
        
        val taskList = findViewById<TableLayout>(R.id.taskList)
        colorRows(taskList)
        
        findViewById<LinearLayout>(R.id.categories).forEach { category ->
            val tasks = (category as LinearLayout).getChildAt(TASKS_INDEX) as TableLayout
            category.visibility = if (tasks.isEmpty()) GONE else VISIBLE
            colorRows(tasks)
        }
        
        findViewById<ImageButton>(R.id.restoreButton).visibility =
            if (history.isEmpty()) GONE else VISIBLE
    }
}
