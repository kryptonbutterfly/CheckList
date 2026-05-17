package kryptonbutterfly.checklist.ui

import android.content.Context
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import kryptonbutterfly.checklist.HistoryActivity
import kryptonbutterfly.checklist.R
import kryptonbutterfly.checklist.actions.CreateTask
import kryptonbutterfly.checklist.actions.DeleteTask
import kryptonbutterfly.checklist.actions.MoveTask
import kryptonbutterfly.checklist.persistence.data
import java.util.Collections

private const val ANIMATION_DURATION: Long = 50

class TaskAdapter<E>(
	private val context: E,
	val tasks: ArrayList<String>,
	private val category: Long,
	private val listName: String,
	private val variant: TaskVariants): RecyclerView.Adapter<TaskAdapter.ViewHolder>() where E: Context, E: HistoryActivity {
	class ViewHolder(view: View): RecyclerView.ViewHolder(view), ItemTouchViewHolder {
		val taskText: TextView = view.findViewById(R.id.taskText)
		val restoreButton: ImageButton = view.findViewById(R.id.buttonNotDone)
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
		if (variant == TaskVariants.TODO_MARKABLE)
			context.getDrawable(R.drawable.unchecked_box)?.also { holder.deleteButton.setImageDrawable(it) }
		
		holder.deleteButton.setOnClickListener {
			val list = data(context).currentList()
			val doneIndex: Int? = if (list.markDone) list.done[category]?.size ?: -1 else null
			
			val desc = holder.taskText.text.toString()
			val currIndex = holder.bindingAdapterPosition
			context.event(if (variant == TaskVariants.DONE)
				DeleteTask(desc, listName, category, -1, currIndex)
			else DeleteTask(desc, listName, category, currIndex, doneIndex))
		}
		
		if (variant == TaskVariants.DONE) {
			holder.restoreButton.visibility = VISIBLE
			holder.restoreButton.setOnClickListener {
				context.event(CreateTask(holder.taskText.text.toString(), listName, category, -1, holder.bindingAdapterPosition))
			}
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
