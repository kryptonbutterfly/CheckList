package de.tinycodecrank.checklist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
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