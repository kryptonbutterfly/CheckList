package de.tinycodecrank.checklist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

const val TASK_DESCRIPTION = "TASK"
const val TASK_ID = "TASK_ID"

class CreateTaskActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)
        val taskDescription = findViewById<TextInputEditText>(R.id.taskDescription)
        intent.getStringExtra(TASK_DESCRIPTION)?.let { description ->
            taskDescription.setText(description)
        }
        taskDescription.requestFocus()
        taskDescription.postDelayed( {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(taskDescription, InputMethodManager.SHOW_FORCED)
        }, 0)
    }

    fun onApply(view: View) {
        val taskDescription = findViewById<TextInputEditText>(R.id.taskDescription)
        val result = Intent()
        result.putExtra(TASK_DESCRIPTION, taskDescription.text.toString())

        if (intent.hasExtra(TASK_ID))
            result.putExtra(TASK_ID, intent.getIntExtra(TASK_ID, -1))

        setResult(Activity.RESULT_OK, result)
        finish()
    }

    fun onCancel(view: View) {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}