package kryptonbutterfly.checklist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SwitchCompat
import kryptonbutterfly.checklist.persistence.data
import kryptonbutterfly.checklist.persistence.settings

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    
        val settings = settings(this)
        
        val undoMaxSizeSpinner = findViewById<NumberPicker>(R.id.spinnerMaxUndoSize)
        undoMaxSizeSpinner.minValue = 0
        undoMaxSizeSpinner.maxValue = 0x40
        undoMaxSizeSpinner.value = settings.undoLength

        findViewById<SwitchCompat>(R.id.switchTrackCreate).isChecked = settings.trackCreate
        findViewById<SwitchCompat>(R.id.switchTrackRename).isChecked = settings.trackRename
        findViewById<SwitchCompat>(R.id.switchTrackMove).isChecked = settings.trackMove
        findViewById<SwitchCompat>(R.id.switchTrackDelete).isChecked = settings.trackDelete

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                Log.i("BACK PRESS", "Back pressed in Settings Activity")
                persist()
            }
        })
    }

    private fun persist() {
        val settings = settings(this)
        settings.undoLength = findViewById<NumberPicker>(R.id.spinnerMaxUndoSize).value
        settings.trackCreate = findViewById<SwitchCompat>(R.id.switchTrackCreate).isChecked
        settings.trackRename = findViewById<SwitchCompat>(R.id.switchTrackRename).isChecked
        settings.trackMove = findViewById<SwitchCompat>(R.id.switchTrackMove).isChecked
        settings.trackDelete = findViewById<SwitchCompat>(R.id.switchTrackDelete).isChecked

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
}
