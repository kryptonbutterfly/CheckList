package kryptonbutterfly.checklist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.size
import kryptonbutterfly.checklist.Constants.ACTION
import kryptonbutterfly.checklist.Constants.CREATE_TASK
import kryptonbutterfly.checklist.Constants.DESCRIPTION
import kryptonbutterfly.checklist.Constants.HTML_POSTFIX
import kryptonbutterfly.checklist.Constants.HTML_PREFIX
import kryptonbutterfly.checklist.Constants.INDEX
import kryptonbutterfly.checklist.Constants.MOVE_TASK
import kryptonbutterfly.checklist.Constants.SETTINGS
import kryptonbutterfly.checklist.Constants.TEXT_COLUMN
import kryptonbutterfly.checklist.actions.*
import kryptonbutterfly.checklist.misc.Stack
import kryptonbutterfly.checklist.persistence.*

const val REQUEST_PERMISSION_CODE = 0

class MainActivity : AppCompatActivity(), DeleteAllDialog.DialogListener {

    private var light: Int = 0
    private var dark: Int = 0
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

    private val getExport =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK)
                result.data?.data?.let(this::export)
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
        this.light = ContextCompat.getColor(this, R.color.light)
        this.dark = ContextCompat.getColor(this, R.color.dark)
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

        updateUI()
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

    fun onExportClick(@Suppress("UNUSED_PARAMETER") view: View) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
        else
            openFilePicker()
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
        event(DeleteAll(taskList.size))
    }

    fun onRestoreClick(@Suppress("UNUSED_PARAMETER") view: View) {
        history.remove()?.let(this::redo)
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

    private fun openFilePicker(){
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "**/*.md"
        intent.putExtra(Intent.EXTRA_TITLE, "tasks.md")
        getExport.launch(intent)
    }

    private fun export(uri: Uri) {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        val sb = StringBuilder()
        taskList.forEach {
            val text = SpannableString(((it as TableRow)[TEXT_COLUMN] as TextView).text.toString())
            val html = Html.toHtml(text, Html.FROM_HTML_MODE_LEGACY)
            val markdown = html.substring(HTML_PREFIX.length, html.length - HTML_POSTFIX.length)
            sb.append(" * ").append(markdown).append("\n")
        }
        val result = sb.toString()
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { br ->
            Log.i("EXPORTING", "File: ${uri}\tdata:\n${result}")
            br.write(result)
        }
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

    private fun createTask(action: CreateTask) {
        Log.i(CREATE_TASK, "Adding task @ ${action.index}")
        val taskList = findViewById<TableLayout>(R.id.taskList)

        val i = action.index.coerceIn(0, taskList.size)

        val taskDescTmpl = findViewById<TextView>(R.id.taskDescriptionTextTemplate)

        val row = TableRow(applicationContext)
        taskList.addView(row, i)
        row.layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        row.setPadding(16, 0, 16, 0)

        val textView = TextView(applicationContext)
        row.addView(textView)
        textView.layoutParams = taskDescTmpl.layoutParams
        textView.text = action.description
        textView.setTextColor(taskDescTmpl.textColors)
        textView.setPadding(0,8,0,8)
        textView.setOnClickListener {
            editTask(taskList.indexOfChild(row), textView.text.toString())
        }

        val deleteButton = ImageButton(applicationContext)
        row.addView(deleteButton)
        val deleteLayout = TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        deleteLayout.gravity = Gravity.CENTER_VERTICAL
        deleteButton.layoutParams = deleteLayout
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
        updateUI()
    }

    private fun redo(action: Action<*>) {
        when (action) {
            is CreateTask -> deleteTask(action.inverse())
            is RenameTask -> renameTask(action.inverse())
            is MoveTask -> moveTask(action.inverse())
            is DeleteTask -> createTask(action.inverse())
            is DeleteAll -> {}
        }
        updateUI()
    }

    private fun updateUI() {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        for (i in 0 until taskList.size)
            (taskList[i] as TableRow)
                .setBackgroundColor(if (i % 2 == 0) dark else light)
        findViewById<ImageButton>(R.id.restoreButton).visibility =
            if (history.isEmpty()) View.INVISIBLE else View.VISIBLE
    }
}
