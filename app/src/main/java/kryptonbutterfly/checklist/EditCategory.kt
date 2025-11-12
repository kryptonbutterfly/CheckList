package kryptonbutterfly.checklist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.google.android.material.textfield.TextInputEditText
import kryptonbutterfly.checklist.Constants.CATEGORY
import kryptonbutterfly.checklist.persistence.Category
import kryptonbutterfly.checklist.persistence.cache
import kryptonbutterfly.checklist.persistence.data

class EditCategory : ComponentActivity() {
	private var category: Category? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_category)
		val categoryId = intent.getLongExtra(CATEGORY, Constants.UNCATEGORIZED)
		val data = data(this)
		data.categories[categoryId]?.also { category ->
			this.category = category
			val cache = cache(this)
			category.icon?.let { cache.iconMap[it] }
				?.also { findViewById<ImageView>(R.id.categoryIcon).setImageBitmap(it) }
			val categoryName = findViewById<TextInputEditText>(R.id.categoryName)
			categoryName.setText(category.name)
		}?: run {
			Log.i("EDIT_CATEGORY", "Category not found … exiting.")
			cancel()
		}
	}
	
	fun onApply(@Suppress("UNUSED_PARAMETER") view: View) {
		Log.i("EDIT_CATEGORY", "Apply")
		val name = findViewById<TextInputEditText>(R.id.categoryName).text.toString()
		category?.also {category ->
			val iconName = category.icon
			if (name.isNotBlank() && name != category.name) {
				val data = data(this)
				val cat = Category(category.id, name, iconName)
				data.categories[category.id]
				val result = Intent()
				result.putExtra(CATEGORY, cat)
				setResult(RESULT_OK, result)
				finish()
			} else
				cancel()
		}?: { cancel() }
	}
	
	fun onCancel(@Suppress("UNUSED_PARAMETER") view: View) {
		Log.i("EDIT_CATEGORY", "Cancel")
		cancel()
	}
	
	private fun cancel() {
		setResult(RESULT_CANCELED)
		finish()
	}
}