package kryptonbutterfly.checklist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kryptonbutterfly.checklist.Constants.CATEGORY
import kryptonbutterfly.checklist.persistence.Category
import kryptonbutterfly.checklist.persistence.cache
import kryptonbutterfly.checklist.persistence.data

class CreateCategory : AppCompatActivity() {
	private var iconName : String? = null
	private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
			uri?.let {
				contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				val view = findViewById<ImageView>(R.id.categoryIcon)
				val cache = cache(this)
				Log.i("CREATE_CATEGORY", "uri $it")
				cache.addIcon(this, it)?.
					also { name ->
						Log.i("CREATE_CATEGORY", "icon name: $name")
						iconName = name
						view.setImageBitmap(cache.iconMap[name])
					}?:
					run {
						Log.i("CREATE_CATEGORY", "icon aborted")
						iconName = null
						view.setImageIcon(null)
					}
			}
		}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_create_category)
	}
	
	fun onApply(@Suppress("UNUSED_PARAMETER") view: View) {
		val name = findViewById<TextInputEditText>(R.id.categoryName).text.toString()
		Log.i("CREATE_CATEGORY", "Apply name $name, icon $iconName")
		if (!name.isBlank()) {
			val result = Intent()
			val data = data(this)
			val cat = Category(data, name, iconName)
			result.putExtra(CATEGORY, cat)
			setResult(RESULT_OK, result)
			finish()
		}
		
		setResult(RESULT_CANCELED)
		finish()
	}
	
	fun onCancel(@Suppress("UNUSED_PARAMETER") view: View) {
		Log.i("CREATE_CATEGORY", "Cancel")
		setResult(RESULT_CANCELED)
		finish()
	}
	
	fun onSelectIcon(@Suppress("UNUSED_PARAMETER") view: View) {
		openFilePicker()
	}
	
	private fun openFilePicker() {
		pickImage.launch(arrayOf("image/*"))
	}
}