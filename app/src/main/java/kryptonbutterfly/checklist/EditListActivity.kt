package kryptonbutterfly.checklist

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import kryptonbutterfly.checklist.Constants.LIST_NAME
import kryptonbutterfly.checklist.misc.WindowInsetsAdapter
import kryptonbutterfly.checklist.persistence.Category
import kryptonbutterfly.checklist.persistence.CheckList
import kryptonbutterfly.checklist.persistence.cache
import kryptonbutterfly.checklist.persistence.data
import java.util.Objects
import java.util.stream.Collectors
import kotlin.streams.asStream

class EditListActivity : ComponentActivity() {
	private var editTarget: String? = null
	private lateinit var listNameInput: TextInputEditText
	private lateinit var buttonEditListAccept: Button
	private lateinit var trackDoneSwitch: SwitchCompat
	private lateinit var filterCategories: SwitchCompat
	private lateinit var categoriesFilterList: LinearLayout
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_edit_list)
		val root = findViewById<View>(R.id.editListRoot)
		ViewCompat.setOnApplyWindowInsetsListener(root, WindowInsetsAdapter())
		
		buttonEditListAccept = findViewById(R.id.buttonEditListAccept)
		listNameInput = findViewById(R.id.listName)
		
		trackDoneSwitch = findViewById(R.id.trackDoneSwitch)
		filterCategories = findViewById(R.id.filterCategoriesSwitch)
		categoriesFilterList = findViewById(R.id.categoriesFilterList)
		
		listNameInput.doAfterTextChanged { buttonEditListAccept.isEnabled = isValidName() }
		
		val data = data(this)
		if (intent.hasExtra(LIST_NAME)) {
			val listName = intent.getStringExtra(LIST_NAME)
			editTarget = listName
			listNameInput.setText(listName)
			val list = data.lists[listName]
			list?.also { list ->
				list.markDone.also { trackDoneSwitch.isChecked = it }
				val filter = list.visCategories != null
				filterCategories.isChecked = filter
				updateCategoriesFilterVis()
			}
			createCategories(categoriesFilterList, data.categories.values, list?.visCategories)
		}
		else
			createCategories(categoriesFilterList, data.categories.values, null)
	}
	
	private fun createCategories(parent: ViewGroup, categories: Collection<Category>, vis: HashSet<Long>?) {
		for (category in categories)
			createCategory(parent, category, vis?.contains(category.id)?:false)
	}
	
	private fun createCategory(parent: ViewGroup, cat: Category, vis: Boolean) {
		val category = this.layoutInflater.inflate(R.layout.category_filter_item, parent, false)
		category.tag = cat.id
		
		val icon = category.findViewById<ImageView>(R.id.catFilterIcon)
		cat.icon?.let { cache(this).iconMap[it] }?.
			also(icon::setImageBitmap)
		
		val text = category.findViewById<TextView>(R.id.catFilterTitle)
		text.text = cat.name
		
		val check = category.findViewById<CheckBox>(R.id.checkFilterCategory)
		check.isChecked = vis
		parent.addView(category)
		Log.d("CATEGORY_FILTER", "text: ${text.text}, icon: ${cat.icon}, vis: $vis")
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
		val markDone = trackDoneSwitch.isChecked
		data.currentList = listName
		
		val filters = collectVisible()
		
		editTarget?.let {
			data.lists.remove(it)
		}?.also { list ->
				list.markDone = markDone
				data.lists[listName] = list
				list.visCategories = filters
			}?:also { data.lists.put(listName, CheckList(markDone = markDone, visCategories = filters)) }
	}
	
	private fun collectVisible() :HashSet<Long>? {
		if (!filterCategories.isChecked)
			return null
		return categoriesFilterList.children.asStream()
			.map { it as? ConstraintLayout }
			.filter { it?.findViewById<CheckBox>(R.id.checkFilterCategory)?.isChecked?:false }
			.map { it?.tag as? Long }
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(::HashSet))
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
	
	fun onToggleFilterCategories(@Suppress("UNUSED_PARAMETER") view: View) {
		updateCategoriesFilterVis()
	}
	
	private fun updateCategoriesFilterVis() {
		categoriesFilterList.visibility = if (filterCategories.isChecked) VISIBLE else GONE
	}
}