package kryptonbutterfly.checklist

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.size
import kryptonbutterfly.checklist.Constants.CREATE_TASK
import kryptonbutterfly.checklist.Constants.MOVE_DOWN
import kryptonbutterfly.checklist.Constants.MOVE_UP
import kryptonbutterfly.checklist.Constants.TASK_DESCRIPTION
import kryptonbutterfly.checklist.Constants.TASK_ID
import kryptonbutterfly.checklist.Constants.TEXT_COLUMN
import kryptonbutterfly.checklist.Constants.UNDELETE_TASK
import kryptonbutterfly.checklist.persistence.Data
import kryptonbutterfly.checklist.persistence.load
import kryptonbutterfly.checklist.persistence.save

class MainActivity : AppCompatActivity(), DeleteAllDialog.DialogListener {
    private val roundedCorners = Drawable.createFromPath("@drawable/rounded_corner")

    private var lastDeleted : DeletedTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        if (findViewById<TableLayout>(R.id.taskList).size != 0)
            return
        load(this) {
            Log.i("ON RESUME", "initializing from data: $it")
            it.tasks.forEach { text -> createTask(text) }
            if (it.deleted.isNotEmpty())
                setLastDeleted(it.deleted.last())
        }
    }

    override fun onPause() {
        super.onPause()
        val taskList = findViewById<TableLayout>(R.id.taskList)
        save(this) { data ->
            taskList.forEach {
                data.tasks.add(((it as TableRow)[TEXT_COLUMN] as TextView).text.toString())
            }
            this.lastDeleted?.let(data.deleted::add)
        }
    }

    override fun onDialogPositiveClick() {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        taskList.removeAllViews()
        setLastDeleted(null)
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result:ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK)
            result.data?.let { data ->
                data.getStringExtra(TASK_DESCRIPTION)?.let { description ->
                    if (data.hasExtra(TASK_ID))
                        setDescription(description, data.getIntExtra(TASK_ID, -1))
                    else
                        createTask(description)
                }
            }
    }

    fun onAddClick(@Suppress("UNUSED_PARAMETER") view:View) {
        val intent = Intent(this, CreateTaskActivity::class.java)
        intent.putExtra(TASK_DESCRIPTION, "")
        getContent.launch(intent)
    }

    fun onDeleteAllClick(@Suppress("UNUSED_PARAMETER")view:View) {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        if (taskList.size > 0) {
            DeleteAllDialog().show(supportFragmentManager, "DeleteAllDialog")
        }
    }

    fun onRestoreClick(@Suppress("UNUSED_PARAMETER") view:View) {
        this.lastDeleted?.let { t -> undeleteTask(t) }
    }

    private fun setDescription(description:String, taskId:Int) {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        if (taskId in IntRange(0, taskList.childCount - 1)) {
            val taskEntry = taskList[taskId] as TableRow
            val text = taskEntry[TEXT_COLUMN] as TextView
            text.text = description
        }
        else
            Log.e("OutOfRange", "The taskId: $taskId is not contained in the current taskList [0 ${taskList.childCount})")
    }

    private fun createTask(description:CharSequence) {
        Log.i(CREATE_TASK, "Creating new task.")
        val taskList = findViewById<TableLayout>(R.id.taskList)
        createTask(taskList, description, taskList.size)
    }

    private fun undeleteTask(task: DeletedTask) {
        Log.i(UNDELETE_TASK, "Undeleting task")
        val taskList = findViewById<TableLayout>(R.id.taskList)
        createTask(taskList, task.description, task.index)
    }

    private fun setLastDeleted(task: DeletedTask?) {
        findViewById<ImageButton>(R.id.restoreButton).visibility =
            if (task == null) View.INVISIBLE else View.VISIBLE
        this.lastDeleted = task
    }

    private fun createTask(taskList: TableLayout, description:CharSequence, index: Int) {
        val i = index.coerceIn(0, taskList.size)
        Log.i(CREATE_TASK, "Adding task @ $i")

        val taskDescTmpl = findViewById<TextView>(R.id.taskDescriptionTextTemplate)

        val row = TableRow(applicationContext)
        taskList.addView(row, i)
        row.layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        val textView = TextView(applicationContext)
        row.addView(textView)
        textView.layoutParams = TableRow.LayoutParams(0, MATCH_PARENT, 1f)
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.text = description
        textView.setTextColor(taskDescTmpl.textColors)
        textView.setOnClickListener {
            val intent = Intent(this, CreateTaskActivity::class.java)
            intent.putExtra(TASK_ID, taskList.indexOfChild(row))
            intent.putExtra(TASK_DESCRIPTION, textView.text)
            getContent.launch(intent)
        }

        val deleteButton = ImageButton(applicationContext)
        row.addView(deleteButton)
        deleteButton.layoutParams = TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        deleteButton.setPadding(6,6,6,6)
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete)
        deleteButton.background = roundedCorners
        deleteButton.setOnClickListener {
            setLastDeleted(DeletedTask(taskList.indexOfChild(row), textView.text))
            taskList.removeView(row)
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
        buttonUp.setOnClickListener {
            moveUp(taskList, row)
        }

        val buttonDown = ImageButton(applicationContext)
        moveView.addView(buttonDown)
        buttonDown.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT, 0.5f)
        buttonDown.setImageResource(android.R.drawable.arrow_down_float)
        buttonDown.background = roundedCorners
        buttonDown.setOnClickListener {
            moveDown(taskList, row)
        }
        setLastDeleted(null)
    }

    private fun moveUp(taskList: TableLayout, row: TableRow) {
        val index = taskList.indexOfChild(row)
        Log.i(MOVE_UP, "Moving row $index to ${index - 1}")
        if (index <= 0)
            return

        val previous = taskList[index - 1] as TableRow
        val prevText = previous[TEXT_COLUMN] as TextView
        val currText = row[TEXT_COLUMN] as TextView
        val swap = prevText.text
        prevText.text = currText.text
        currText.text = swap
    }

    private fun moveDown(taskList: TableLayout, row: TableRow) {
        val index = taskList.indexOfChild(row)
        Log.i(MOVE_DOWN, "Moving row $index to ${index + 1}")
        if (index >= taskList.size - 1)
            return

        val next = taskList[index + 1] as TableRow
        val nextText = next[TEXT_COLUMN] as TextView
        val currText = row[TEXT_COLUMN] as TextView
        val swap = nextText.text
        nextText.text = currText.text
        currText.text = swap
    }
}
