package kryptonbutterfly.checklist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import kryptonbutterfly.checklist.backup.Backup
import kryptonbutterfly.checklist.persistence.data
import kryptonbutterfly.checklist.persistence.settings
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsActivity : ComponentActivity() {
    
    private val getCreateBackup =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result.data?.data?.also { Backup.create(this, it) }
        }
    
    private val getRestoreBackup =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.also { Backup.restore(this, it) }
        }
    private lateinit var spinnerMaxUndo: NumberPicker
    private lateinit var switchTrackCreate: SwitchCompat
    private lateinit var switchTrackRename: SwitchCompat
    private lateinit var switchTrackMove: SwitchCompat
    private lateinit var switchTrackDelete: SwitchCompat
    private lateinit var switchSkipDuplicates: SwitchCompat
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        spinnerMaxUndo = findViewById(R.id.spinnerMaxUndoSize)
        switchTrackCreate = findViewById(R.id.switchTrackCreate)
        switchTrackRename = findViewById(R.id.switchTrackRename)
        switchTrackMove = findViewById(R.id.switchTrackMove)
        switchTrackDelete = findViewById(R.id.switchTrackDelete)
        switchSkipDuplicates = findViewById(R.id.switchSkipDuplicates)
    
        val settings = settings(this)
        
        spinnerMaxUndo.minValue = 0
        spinnerMaxUndo.maxValue = 0x40
        spinnerMaxUndo.value = settings.undoLength

        
        switchTrackCreate.isChecked = settings.trackCreate
        switchTrackRename.isChecked = settings.trackRename
        switchTrackMove.isChecked = settings.trackMove
        switchTrackDelete.isChecked = settings.trackDelete
        switchSkipDuplicates.isChecked = settings.skipExistingTasks

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                Log.i("BACK PRESS", "Back pressed in Settings Activity")
                persist()
            }
        })
    }

    private fun persist() {
        val settings = settings(this)
        settings.undoLength = spinnerMaxUndo.value
        settings.trackCreate = switchTrackCreate.isChecked
        settings.trackRename = switchTrackRename.isChecked
        settings.trackMove = switchTrackMove.isChecked
        settings.trackDelete = switchTrackDelete.isChecked
        settings.skipExistingTasks = switchSkipDuplicates.isChecked

        val result = Intent()
        setResult(RESULT_OK, result)
        finish()
    }
    
    fun onApply(@Suppress("UNUSED_PARAMETER")view: View) {
        persist()
    }

    fun onCancel(@Suppress("UNUSED_PARAMETER")view: View) {
        setResult(RESULT_CANCELED)
        finish()
    }
    
    fun onCleanup(@Suppress("UNUSED_PARAMETER")view: View) {
        data(this).prune()
    }
    
    fun onCreateBackup(@Suppress("UNUSED_PARAMETER")view: View) {
        val dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val fileName = "CheckList-Backup_$dateTime.zip"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/zip"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, fileName)
            flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        getCreateBackup.launch(intent)
    }
    
    fun onRestoreBackup(@Suppress("UNUSED_PARAMETER")view: View) {
        getRestoreBackup.launch(arrayOf("application/zip"))
    }
}
