package org.fossify.voicerecorder.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import org.fossify.voicerecorder.R
import org.fossify.voicerecorder.activities.BackgroundRecordActivity

/**
 * Quick Settings tile that starts and stops a recording, so it can be
 * triggered from the notification shade, including on the lock screen.
 */
class RecorderTileService : TileService() {
    companion object {
        fun requestUpdate(context: Context) {
            requestListeningState(
                context,
                ComponentName(context, RecorderTileService::class.java)
            )
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (RecorderService.isRunning) {
            stopService(Intent(this, RecorderService::class.java))
        } else {
            startRecording()
        }
        updateTile()
    }

    private fun startRecording() {
        try {
            ContextCompat.startForegroundService(this, Intent(this, RecorderService::class.java))
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            // The system refused to start the service in the background, so go through the
            // activity that handles the notification permission and starts the recording.
            openRecordActivity()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openRecordActivity() {
        val intent = Intent(this, BackgroundRecordActivity::class.java).apply {
            action = BackgroundRecordActivity.RECORD_INTENT_ACTION
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (RecorderService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, org.fossify.commons.R.drawable.ic_microphone_vector)
        tile.label = getString(R.string.recorder)
        tile.updateTile()
    }
}
