package kryptonbutterfly.checklist.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import java.util.function.Function

class SimpleConfirmDialog(val createDialog: Function<FragmentActivity, AlertDialog>) :
	DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return activity?.let { this.createDialog.apply(it) }
			?: throw IllegalStateException("Activity must not be null!")
	}
}