package kryptonbutterfly.checklist.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import kryptonbutterfly.checklist.R

class AttribCategoryID(context: Context, attrs: AttributeSet) : View(context, attrs) {
	var categoryID: Int
	
	init {
		val attrib = context.theme.obtainStyledAttributes(attrs, R.styleable.AttribCategoryID, 0, 0)
		try {
			categoryID = attrib.getInt(R.styleable.AttribCategoryID_categoryId, Integer.MIN_VALUE)
		} finally {
			attrib.recycle()
		}
	}
}
