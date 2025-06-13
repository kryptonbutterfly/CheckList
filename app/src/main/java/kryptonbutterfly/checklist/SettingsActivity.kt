package kryptonbutterfly.checklist

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import androidx.appcompat.widget.SwitchCompat
import kryptonbutterfly.checklist.Constants.SETTINGS
import kryptonbutterfly.checklist.persistence.Settings

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        intent.getSerializableExtra(SETTINGS)?.let { it ->
            Log.i("SETTINGS INIT", "$SETTINGS: $it")
            this.settings = it as Settings
        }

        val undoMaxSizeSpinner = findViewById<NumberPicker>(R.id.spinnerMaxUndoSize)
        undoMaxSizeSpinner.minValue = 0
        undoMaxSizeSpinner.maxValue = 0x40
        undoMaxSizeSpinner.value = this.settings.undoLength

        findViewById<SwitchCompat>(R.id.switchTrackCreate).isChecked = settings.trackCreate
        findViewById<SwitchCompat>(R.id.switchTrackRename).isChecked = settings.trackRename
        findViewById<SwitchCompat>(R.id.switchTrackMove).isChecked = settings.trackMove
        findViewById<SwitchCompat>(R.id.switchTrackDelete).isChecked = settings.trackDelete
    }

    private fun persist() {
        settings.undoLength = findViewById<NumberPicker>(R.id.spinnerMaxUndoSize).value
        settings.trackCreate = findViewById<SwitchCompat>(R.id.switchTrackCreate).isChecked
        settings.trackRename = findViewById<SwitchCompat>(R.id.switchTrackRename).isChecked
        settings.trackMove = findViewById<SwitchCompat>(R.id.switchTrackMove).isChecked
        settings.trackDelete = findViewById<SwitchCompat>(R.id.switchTrackDelete).isChecked

        val result = Intent()
        result.putExtra(SETTINGS, this.settings)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.i("BACK PRESS", "Back pressed in Settings Activity")
        persist()
    }

    fun onApply(@Suppress("UNUSED_PARAMETER")view: View) {
        persist()
    }

    fun onCancel(@Suppress("UNUSED_PARAMETER")view: View) {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}
