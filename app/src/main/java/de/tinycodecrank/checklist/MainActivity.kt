package de.tinycodecrank.checklist

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.get
import androidx.core.view.size
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

private const val TASKS_LIST_FILE = "TasksListData.txt"
class MainActivity : AppCompatActivity(), DeleteAllDialog.DialogListener {
    private val roundedCorners = Drawable.createFromPath("@drawable/rounded_corner")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        if (findViewById<TableLayout>(R.id.taskList).size == 0 && File(this.filesDir, TASKS_LIST_FILE).exists()) {
            ObjectInputStream(openFileInput(TASKS_LIST_FILE)).use { iStream ->
                if (iStream.available() > 0) {
                    repeat(iStream.readInt()) { createTask(iStream.readUTF()) }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        val taskList = findViewById<TableLayout>(R.id.taskList)
        ObjectOutputStream(openFileOutput(TASKS_LIST_FILE, MODE_PRIVATE)).use {
                it.writeInt(taskList.size)
                repeat(taskList.size) { index -> it.writeUTF(((taskList[index] as TableRow)[1] as TextView).text.toString())
            }
        }
    }

    override fun onDialogPositiveClick() {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        taskList.removeAllViews()
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

    fun onAddClick(view:View) {
        val intent = Intent(this, CreateTaskActivity::class.java)
        intent.putExtra(TASK_DESCRIPTION, "")
        getContent.launch(intent)
    }

    fun onDeleteAllClick(view:View) {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        if (taskList.size > 0) {
            DeleteAllDialog().show(supportFragmentManager, "DeleteAllDialog")
        }
    }

    private fun setDescription(description:String, taskId:Int) {
        val taskList = findViewById<TableLayout>(R.id.taskList)
        if (taskId in IntRange(0, taskList.childCount - 1)) {
            val taskEntry = taskList[taskId] as TableRow
            val text = taskEntry[1] as TextView
            text.text = description
        }
        else
            Log.e("OutOfRange", "The taskId: $taskId is not contained in the current taskList [0 ${taskList.childCount})")
    }

    private fun createTask(description:String) {
        val taskDescTmpl = findViewById<TextView>(R.id.taskDescriptionTextTemplate)

        val taskList = findViewById<TableLayout>(R.id.taskList)
        val row = TableRow(applicationContext)
        taskList.addView(row, taskList.childCount)
        row.layoutParams = TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val deleteButton = ImageButton(applicationContext)
        row.addView(deleteButton)
        deleteButton.layoutParams = TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete)
        deleteButton.background = roundedCorners
        deleteButton.setOnClickListener {
            taskList.removeView(row)
        }

        val textView = TextView(applicationContext)
        row.addView(textView)
        textView.layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.text = description
        textView.setTextColor(taskDescTmpl.textColors)
        textView.setOnClickListener {
            val intent = Intent(this, CreateTaskActivity::class.java)
            intent.putExtra(TASK_ID, taskList.indexOfChild(row))
            intent.putExtra(TASK_DESCRIPTION, textView.text)
            getContent.launch(intent)
        }

        val moveView = LinearLayout(applicationContext)
        row.addView(moveView)
        moveView.orientation = LinearLayout.VERTICAL
        moveView.layoutParams = TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        moveView.weightSum = 0f

        val buttonUp = ImageButton(applicationContext)
        moveView.addView(buttonUp)
        buttonUp.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        buttonUp.setImageResource(android.R.drawable.arrow_up_float)
        buttonUp.background = roundedCorners
        buttonUp.setOnClickListener {
            moveUp(taskList, row)
        }

        val buttonDown = ImageButton(applicationContext)
        moveView.addView(buttonDown)
        buttonDown.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        buttonDown.setImageResource(android.R.drawable.arrow_down_float)
        buttonDown.background = roundedCorners
        buttonDown.setOnClickListener {
            moveDown(taskList, row)
        }
    }

    private fun moveUp(taskList: TableLayout, row: TableRow) {
        val index = taskList.indexOfChild(row)
        Log.i("MOVE UP", "Moving row $index to ${index - 1}")
        if (index > 0) {
            val previous = taskList[index - 1] as TableRow
            val prevText = previous[1] as TextView
            val currText = row[1] as TextView
            val swap = prevText.text
            prevText.text = currText.text
            currText.text = swap
        }
    }

    private fun moveDown(taskList: TableLayout, row: TableRow) {
        val index = taskList.indexOfChild(row)
        Log.i("MOVE DOWN", "Moving row $index to ${index + 1}")
        if (index < taskList.size - 1) {
            val next = taskList[index + 1] as TableRow
            val nextText = next[1] as TextView
            val currText = row[1] as TextView
            val swap = nextText.text
            nextText.text = currText.text
            currText.text = swap
        }
    }
}