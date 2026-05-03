package kryptonbutterfly.checklist.ui

import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import kryptonbutterfly.checklist.MainActivity
import kryptonbutterfly.checklist.R
import kryptonbutterfly.checklist.actions.DeleteTask
import kryptonbutterfly.checklist.actions.MoveTask
import java.util.Collections

private const val ANIMATION_DURATION: Long = 50

class TaskAdapter(
	private val context: MainActivity,
	val tasks: ArrayList<String>,
	private val category: Long,
	private val listName: String): RecyclerView.Adapter<TaskAdapter.ViewHolder>(){
	class ViewHolder(view: View): RecyclerView.ViewHolder(view), ItemTouchViewHolder {
		val taskText: TextView = view.findViewById(R.id.taskText)
		val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
		
		private val itemGlow = ContextCompat.getDrawable(view.context, R.drawable.item_glow)
		
		override fun onItemSelected() {
			itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
			itemView.animate().scaleX(1.03F).scaleY(1.03F).setDuration(ANIMATION_DURATION).start()
			ViewCompat.setElevation(itemView, 12F)
			itemView.alpha = 0.95F
			itemView.foreground = itemGlow
			itemView.foreground.alpha = 192
		}
		
		override fun onItemClear() {
			itemView.animate().scaleX(1F).scaleY(1F).setDuration(ANIMATION_DURATION).start()
			ViewCompat.setElevation(itemView, 0F)
			itemView.alpha = 1F
			itemView.foreground = null
		}
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(context)
			.inflate(R.layout.task_item, parent, false)
		return ViewHolder(view)
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.taskText.text = tasks[position]
		holder.taskText.setOnClickListener {
			context.editTask(holder.bindingAdapterPosition, category, holder.taskText.text.toString())
		}
		holder.deleteButton.setOnClickListener {
			context.event(DeleteTask(holder.taskText.text.toString(), listName, category,holder.bindingAdapterPosition))
		}
		context.setItemBG(holder.itemView, position)
	}
	
	override fun getItemCount() = tasks.size
	
	fun moveItem(from: Int, to: Int, trackChange: Boolean = true) {
		if (from == to) return
		Log.d("move Item", "moving item from $from to $to")
		Collections.swap(tasks, from, to)
		notifyItemMoved(from, to)
		if (trackChange)
			context.event(MoveTask(from, to, "", category, listName))
	}
	
	fun triggerMove(from: Int, to: Int) {
		if (from == to) return
		context.event(MoveTask(from, to, tasks[from], category, listName))
	}
}
