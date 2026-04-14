package kryptonbutterfly.checklist

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.google.android.material.textfield.TextInputEditText
import kryptonbutterfly.checklist.persistence.CheckList
import kryptonbutterfly.checklist.persistence.data

class EditListActivity : ComponentActivity() {
	private lateinit var listNameInput: TextInputEditText
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_list)
		listNameInput = findViewById(R.id.listName)
	}
	
	fun onApply(@Suppress("UNUSED_PARAMETER") view : View)
	{
		val listName = listNameInput.text.toString()
		val data = data(this)
		if (!listName.isBlank() && !data.lists.containsKey(listName)) {
			data.lists.put(listName, CheckList())
			data.currentList = listName
			setResult(RESULT_OK)
			finish()
		}
		setResult(RESULT_CANCELED)
		finish()
	}
	
	fun onCancel(@Suppress("UNUSED_PARAMETER") view: View) {
		Log.i("CREATE_LIST", "Cancel")
		setResult(RESULT_CANCELED)
		finish()
	}
}