package kryptonbutterfly.checklist.misc

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kryptonbutterfly.checklist.ui.ItemTouchViewHolder
import kryptonbutterfly.checklist.ui.TaskAdapter

fun setAnimatorDurations(animator: RecyclerView.ItemAnimator?) {
	animator?.apply {
		addDuration = 50
		removeDuration = 50
		moveDuration = 50
		changeDuration = 30
	}
}
	
val dragHelper = object : ItemTouchHelper.SimpleCallback(
	ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
) {
	override fun onMove(
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		target: RecyclerView.ViewHolder
	): Boolean {
		val from = viewHolder.bindingAdapterPosition
		val to = target.bindingAdapterPosition
		(recyclerView.adapter as? TaskAdapter<*>)?.triggerMove(from, to)
		return true
	}
	
	override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
		//nothing to do here
	}
	
	override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
		if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) (viewHolder as? ItemTouchViewHolder)?.onItemSelected()
		super.onSelectedChanged(viewHolder, actionState)
	}
	
	override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
		super.clearView(recyclerView, viewHolder)
		(viewHolder as? ItemTouchViewHolder)?.onItemClear()
	}
	
	override fun isItemViewSwipeEnabled() = false
	
	override fun isLongPressDragEnabled() = true
}