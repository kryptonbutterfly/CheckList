package kryptonbutterfly.checklist

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.size
import kryptonbutterfly.checklist.Constants.ACTION
import kryptonbutterfly.checklist.Constants.CREATE_TASK
import kryptonbutterfly.checklist.Constants.DESCRIPTION
import kryptonbutterfly.checklist.Constants.INDEX
import kryptonbutterfly.checklist.Constants.MOVE_TASK
import kryptonbutterfly.checklist.Constants.SETTINGS
import kryptonbutterfly.checklist.Constants.TEXT_COLUMN
import kryptonbutterfly.checklist.actions.*
import kryptonbutterfly.checklist.misc.Stack
import kryptonbutterfly.checklist.persistence.*

class MainActivity : AppCompatActivity(), DeleteAllDialog.DialogListener {
    private val roundedCorners = Drawable.createFromPath("@drawable/rounded_corner")

    private val history = Stack<Action<*>>()
    private var settings: Settings = Settings()

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getSerializableExtra(ACTION)?.let { action ->
                    event(action as Action<*>)
                }
            }
        }

    private val settingsResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getSerializableExtra(SETTINGS)?.let { settings ->
                    this.settings = settings as Settings
                    history.limit = this.settings.undoLength
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        if (findViewById<TableLayout>(R.id.taskList).size != 0)
            return

        this.settings = loadSettings(this)
        history.limit = settings.undoLength

        val data = loadData(this)
        data.tasks.forEach(this::createTask)
        history.backingList = data.history

        updateUndo()
    }

    override fun onPause() {
        super.onPause()
        val taskList = findViewById<TableLayout>(R.id.taskList)

        saveSettings(this, this.settings)

        val data = Data()
        taskList.forEach {
            data.tasks.add(((it as TableRow)[TEXT_COLUMN] as TextView).text.toString())
        }
        data.history = this.history.backingList
        saveData(this, data)
    }

    fun onSettingsClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra(SETTINGS, this.settings)
        settingsResult.launch(intent)
    }

    fun onAddClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, CreateTaskActivity::class.java)
        val taskList = findViewById<TableLayout>(R.id.taskList)
        intent.putExtra(INDEX, taskList.size)
        getContent.launch(intent)
    }

    fun onDeleteAllClick(@Suppress("UNUSED_PARAMETER") view: View) {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        if (taskList.size > 0)
            DeleteAllDialog().show(supportFragmentManager, "DeleteAllDialog")
    }

    override fun onDialogPositiveClick() {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        taskList.removeAllViews()
        clearDeleted()
    }

    fun onRestoreClick(@Suppress("UNUSED_PARAMETER") view: View) {
        history.remove()?.let(this::redo)
    }

    private fun renameTask(action: RenameTask) {
        setDescription(action.new, action.index)
    }

    private fun setDescription(description: String, taskId: Int) {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        if (taskId in IntRange(0, taskList.childCount - 1)) {
            val taskEntry = taskList[taskId] as TableRow
            val text = taskEntry[TEXT_COLUMN] as TextView
            text.text = description
        } else
            Log.e(
                "OutOfRange",
                "The taskId: $taskId is not contained in the current taskList [0 ${taskList.childCount})"
            )
    }

    private fun createTask(description: String) {
        Log.i(CREATE_TASK, "Creating new task.")
        val taskList = findViewById<TableLayout>(R.id.taskList)
        createTask(CreateTask(description, taskList.size))
    }

    private fun clearDeleted() {
        history.clear()
    }

    private fun createTask(action: CreateTask) {
        Log.i(CREATE_TASK, "Adding task @ ${action.index}")
        val taskList = findViewById<TableLayout>(R.id.taskList)

        val i = action.index.coerceIn(0, taskList.size)

        val taskDescTmpl = findViewById<TextView>(R.id.taskDescriptionTextTemplate)

        val row = TableRow(applicationContext)
        taskList.addView(row, i)
        row.layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        val textView = TextView(applicationContext)
        row.addView(textView)
        textView.layoutParams = TableRow.LayoutParams(0, MATCH_PARENT, 1f)
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.text = action.description
        textView.setTextColor(taskDescTmpl.textColors)
        textView.setOnClickListener {
            editTask(taskList.indexOfChild(row), textView.text.toString())
        }

        val deleteButton = ImageButton(applicationContext)
        row.addView(deleteButton)
        deleteButton.layoutParams = TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        deleteButton.setPadding(6, 6, 6, 6)
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete)
        deleteButton.background = roundedCorners
        deleteButton.setOnClickListener {
            event(DeleteTask(textView.text.toString(), taskList.indexOfChild(row)))
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
        buttonUp.setOnClickListener { moveUp(taskList, row) }

        val buttonDown = ImageButton(applicationContext)
        moveView.addView(buttonDown)
        buttonDown.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT, 0.5f)
        buttonDown.setImageResource(android.R.drawable.arrow_down_float)
        buttonDown.background = roundedCorners
        buttonDown.setOnClickListener { moveDown(taskList, row) }
    }

    private fun editTask(index: Int, description: String) {
        val intent = Intent(this, CreateTaskActivity::class.java)
        intent.putExtra(INDEX, index)
        intent.putExtra(DESCRIPTION, description)
        getContent.launch(intent)
    }

    private fun moveUp(taskList: TableLayout, row: TableRow) {
        val index = taskList.indexOfChild(row)
        if (index <= 0)
            return

        val new = index - 1
        val previous = taskList[new] as TableRow
        val prevText = previous[TEXT_COLUMN] as TextView
        event(MoveTask(index, new, prevText.text.toString()))
    }

    private fun moveDown(taskList: TableLayout, row: TableRow) {
        val index = taskList.indexOfChild(row)
        val new = index + 1
        if (index >= taskList.size - 1)
            return

        val next = taskList[new] as TableRow
        val nextText = next[TEXT_COLUMN] as TextView
        event(MoveTask(index, new, nextText.text.toString()))
    }

    private fun deleteTask(action: DeleteTask) {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        taskList.removeViewAt(action.index)
    }

    private fun moveTask(action: MoveTask) {
        Log.i(MOVE_TASK, "Moving row $action.old to $action.new")
        val taskList = findViewById<TableLayout>(R.id.taskList)
        val range = IntRange(0, taskList.size - 1)
        if (action.old !in range) {
            Log.i(MOVE_TASK, "old: ${action.old} is out of range $range")
            return
        }
        if (action.new !in range) {
            Log.i(MOVE_TASK, "new: ${action.new} is out of range $range")
            return
        }

        val curr = taskList[action.old] as TableRow
        val next = taskList[action.new] as TableRow
        val currText = curr[TEXT_COLUMN] as TextView
        val nextText = next[TEXT_COLUMN] as TextView
        val swap = nextText.text
        nextText.text = currText.text
        currText.text = swap
    }

    private fun event(action: Action<*>) {
        when (action) {
            is CreateTask -> {
                createTask(action)
                if (settings.trackCreate)
                    history.add(action)
                else
                    history.clear()
            }
            is RenameTask -> {
                renameTask(action)
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
        updateUndo()
    }

    private fun redo(action: Action<*>) {
        when (action) {
            is CreateTask -> deleteTask(action.inverse())
            is RenameTask -> renameTask(action.inverse())
            is MoveTask -> moveTask(action.inverse())
            is DeleteTask ->  createTask(action.inverse())
            is DeleteAll -> {}
        }
        updateUndo()
    }

    private fun updateUndo() {
        findViewById<ImageButton>(R.id.restoreButton).visibility =
            if (history.isEmpty()) View.INVISIBLE else View.VISIBLE
    }
}
