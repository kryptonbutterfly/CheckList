package kryptonbutterfly.checklist

import android.view.View
import kryptonbutterfly.checklist.actions.Action

interface HistoryActivity {
	fun editTask(index: Int, categoryId: Long, description: String)
	fun event(action: Action<*>)
	fun setItemBG(target: View, pos: Int)
}