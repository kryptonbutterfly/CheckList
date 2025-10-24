package kryptonbutterfly.checklist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kryptonbutterfly.checklist.Constants.ACTION
import kryptonbutterfly.checklist.Constants.CATEGORY
import kryptonbutterfly.checklist.Constants.CREATE_TASK
import kryptonbutterfly.checklist.Constants.DESCRIPTION
import kryptonbutterfly.checklist.Constants.INDEX
import kryptonbutterfly.checklist.Constants.UNCATEGORIZED
import kryptonbutterfly.checklist.actions.ChangeTask
import kryptonbutterfly.checklist.actions.CreateTask
import kryptonbutterfly.checklist.persistence.Category
import kryptonbutterfly.checklist.persistence.IconCache
import kryptonbutterfly.checklist.persistence.cache
import kryptonbutterfly.checklist.persistence.data
import kryptonbutterfly.checklist.ui.SpinnerIconAdapter
import kryptonbutterfly.checklist.ui.SpinnerIconItem


private var lastCategoryId: Long = UNCATEGORIZED
class CreateTaskActivity : AppCompatActivity() {
    private var description: String? = null
    private var categoryId: Long = UNCATEGORIZED
    private var index: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)
        val taskDescription = findViewById<TextInputEditText>(R.id.categoryName)
        
        this.description = intent.getStringExtra(DESCRIPTION)
        this.description?.also(taskDescription::setText)?:
        also {
            this.categoryId = lastCategoryId
        }
        if (intent.hasExtra(CATEGORY))
            this.categoryId = intent.getLongExtra(CATEGORY, Long.MAX_VALUE)
        this.index = intent.getIntExtra(INDEX, -1)
        

        taskDescription.requestFocus()
        taskDescription.postDelayed({
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(taskDescription, InputMethodManager.SHOW_IMPLICIT)
        }, 0)
        
        items.add(SpinnerIconItem(null, "", UNCATEGORIZED))
        val data = data(this)
        val cache = cache(this)
        
        var categoryIndex = 0
        data.categories.values.forEachIndexed { i, cat ->
            addCategory(cache, cat)
            if (cat.id == categoryId)
                categoryIndex = i + 1
        }
        
        val spinner = findViewById<Spinner>(R.id.spinnerCategories)
        spinner.adapter = SpinnerIconAdapter(this, items)
        spinner.setSelection(categoryIndex)
    }
    
    private val items = ArrayList<SpinnerIconItem>()
    
    private fun addCategory(cache: IconCache, category: Category) {
        val icon = cache.iconMap[category.icon]
        items.add(SpinnerIconItem(icon, category.name, category.id))
    }

    fun onApply(@Suppress("UNUSED_PARAMETER") view: View) {
        Log.i(CREATE_TASK, "apply clicked")
        val taskDescription = findViewById<TextInputEditText>(R.id.categoryName)
        val categories = findViewById<Spinner>(R.id.spinnerCategories)
        
        val data = data(this)
        
        val result = Intent()

        val text = taskDescription.text.toString()
        val catNew = (categories.selectedItem as? SpinnerIconItem)?.id ?: UNCATEGORIZED
        if (description != null) {
            val descOld = description as String
            if (catNew == categoryId) {
                lastCategoryId = catNew
                result.putExtra(ACTION, ChangeTask(descOld, categoryId, index, text, catNew, index))
                setResult(RESULT_OK, result)
                finish()
                return
            } else {
                val indNew = data.tasks[catNew]?.size ?: 0
                lastCategoryId = catNew
                result.putExtra(
                    ACTION,
                    ChangeTask(descOld, categoryId, index, text, catNew, indNew)
                )
                setResult(RESULT_OK, result)
                finish()
                return
            }
        }  else {
            lastCategoryId = catNew
            result.putExtra(ACTION, CreateTask(text, catNew, index))
            setResult(RESULT_OK, result)
            finish()
            return
        }
    }

    fun onCancel(@Suppress("UNUSED_PARAMETER") view: View) {
        setResult(RESULT_CANCELED)
        finish()
    }
    
    private val createCategory =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK)
				return@registerForActivityResult
            (result.data?.getSerializableExtra(CATEGORY) as? Category)?.
                also { cat ->
                    val data = data(this)
                    data.categories.put(cat.id, cat)
                    addCategory(cache(this), cat)
                    val categories = findViewById<Spinner>(R.id.spinnerCategories)
                    categories.setSelection(items.size - 1)
                }
        }
    
    
    fun onAddCategory(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, CreateCategory::class.java)
        createCategory.launch(intent)
    }
}