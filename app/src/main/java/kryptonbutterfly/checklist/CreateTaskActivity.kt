package kryptonbutterfly.checklist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kryptonbutterfly.checklist.Constants.ACTION
import kryptonbutterfly.checklist.Constants.DESCRIPTION
import kryptonbutterfly.checklist.Constants.INDEX
import kryptonbutterfly.checklist.actions.CreateTask
import kryptonbutterfly.checklist.actions.RenameTask

class CreateTaskActivity : AppCompatActivity() {
    private var description: String? = null
    private var index: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)
        val taskDescription = findViewById<TextInputEditText>(R.id.taskDescription)

        this.index = intent.getIntExtra(INDEX, -1)
        this.description = intent.getStringExtra(DESCRIPTION)
        taskDescription.setText(this.description)

        taskDescription.requestFocus()
        taskDescription.postDelayed({
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(taskDescription, InputMethodManager.SHOW_FORCED)
        }, 0)
    }

    fun onApply(@Suppress("UNUSED_PARAMETER") view: View) {
        val taskDescription = findViewById<TextInputEditText>(R.id.taskDescription)
        val result = Intent()

        val text = taskDescription.text.toString()
        result.putExtra(ACTION, description?.let {
            RenameTask(it, text, index)
        } ?: CreateTask(text, index))

        setResult(Activity.RESULT_OK, result)
        finish()
    }

    fun onCancel(@Suppress("UNUSED_PARAMETER") view: View) {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}