package kryptonbutterfly.checklist

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import kryptonbutterfly.checklist.Constants.LIST_NAME
import kryptonbutterfly.checklist.persistence.CheckList
import kryptonbutterfly.checklist.persistence.data

class EditListActivity : ComponentActivity() {
	private lateinit var listNameInput: TextInputEditText
	private lateinit var buttonEditListAccept: Button
	private var editTarget: String? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_list)
		
		buttonEditListAccept = findViewById(R.id.buttonEditListAccept)
		listNameInput = findViewById(R.id.listName)
		
		listNameInput.doAfterTextChanged { buttonEditListAccept.isEnabled = isValidName() }
		
		if (intent.hasExtra(LIST_NAME)) {
			val listName = intent.getStringExtra(LIST_NAME)
			editTarget = listName
			listNameInput.setText(listName)
		}
	}
	
	private fun isValidName(): Boolean {
		val listName = listNameInput.text.toString()
		val data = data(this)
		if (listName.isBlank())
			return false
		return listName == editTarget || !data.lists.containsKey(listName)
	}
	
	private fun updateOrCreateList() {
		val data = data(this)
		val listName = listNameInput.text.toString()
		data.currentList = listName
		
		editTarget?.let {
			data.lists.remove(it)
		}?.also { list ->
				data.lists[listName] = list
			}?:also { data.lists.put(listName, CheckList()) }
	}
	
	fun onApply(@Suppress("UNUSED_PARAMETER") view: View) {
		if (isValidName()) {
			updateOrCreateList()
			setResult(RESULT_OK)
		} else setResult(RESULT_CANCELED)
		finish()
	}
	
	fun onCancel(@Suppress("UNUSED_PARAMETER") view: View) {
		Log.i("CREATE_LIST", "Cancel")
		setResult(RESULT_CANCELED)
		finish()
	}
}