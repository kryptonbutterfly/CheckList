package de.tinycodecrank.checklist

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class DeleteAllDialog: DialogFragment() {
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
            AlertDialog.Builder(it).setMessage("Delete all Tasks?")
                .setPositiveButton("Delete") { _, _ -> listener.onDialogPositiveClick() }
                .setNeutralButton("Cancel"){ _, _ -> }
                .create()
        }?: throw IllegalStateException("Activity must not be null!")
    }
}