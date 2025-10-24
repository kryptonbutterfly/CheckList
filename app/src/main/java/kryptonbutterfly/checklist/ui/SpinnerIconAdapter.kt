package kryptonbutterfly.checklist.ui

import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import kryptonbutterfly.checklist.R

class SpinnerIconAdapter(context: ContextWrapper, items: List<SpinnerIconItem>) :
	ArrayAdapter<SpinnerIconItem>(context, 0, items) {
	
	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		return createView(position, convertView, parent)
	}
	
	override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
		return createView(position, convertView, parent)
	}
	
	fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
		val convert = convertView ?:
			LayoutInflater.from(context)
				.inflate(R.layout.spinner_icon_item, parent, false)
		getItem(position)?. also {currentItem ->
			val iconView = convert.findViewById<ImageView>(R.id.spinner_icon)
			val textView = convert.findViewById<TextView>(R.id.spinner_text)
			iconView.setImageBitmap(currentItem.icon)
			textView.text = currentItem.text
		}
		return convert
	}
}