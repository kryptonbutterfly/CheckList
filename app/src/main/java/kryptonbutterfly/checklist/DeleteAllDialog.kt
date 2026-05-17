package kryptonbutterfly.checklist

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import kryptonbutterfly.checklist.Constants.MSG_DELETE_ALL_TASKS
import kryptonbutterfly.checklist.Constants.MSG_MARK_ALL_DONE
import kryptonbutterfly.checklist.Constants.TEXT_CANCEL
import kryptonbutterfly.checklist.Constants.TEXT_DELETE
import kryptonbutterfly.checklist.Constants.TEXT_OK

class DeleteAllDialog(val delete: Boolean): DialogFragment() {
    private lateinit var listener: DialogListener

    interface DialogListener {
        fun onDialogPositiveClick()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as DialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it).setMessage( if(delete) MSG_DELETE_ALL_TASKS else MSG_MARK_ALL_DONE)
                .setPositiveButton(if (delete) TEXT_DELETE else TEXT_OK) { _, _ -> listener.onDialogPositiveClick() }
                .setNeutralButton(TEXT_CANCEL){ _, _ -> }
                .create()
        }?: throw IllegalStateException("Activity must not be null!")
    }
}