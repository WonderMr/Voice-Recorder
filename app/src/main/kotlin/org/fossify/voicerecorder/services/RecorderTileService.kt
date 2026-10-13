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
import org.fossify.voicerecorder.R
import org.fossify.voicerecorder.activities.BackgroundRecordActivity

/**
 * Quick Settings tile that starts and stops a recording, so it can be
 * triggered from the notification shade, including on the lock screen.
 */
class RecorderTileService : TileService() {
    companion object {
        private var listeningInstance: RecorderTileService? = null

        fun requestUpdate(context: Context) {
            // requestListeningState does nothing while the tile is already listening,
            // so a tile that is currently visible has to be updated directly.
            listeningInstance?.updateTile() ?: requestListeningState(
                context,
                ComponentName(context, RecorderTileService::class.java)
            )
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningInstance = this
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        if (listeningInstance == this) {
            listeningInstance = null
        }
    }

    override fun onClick() {
        super.onClick()
        if (RecorderService.isRunning) {
            stopService(Intent(this, RecorderService::class.java))
            // The service is destroyed asynchronously, so don't wait for it to update the tile.
            updateTile(isRecording = false)
        } else {
            // The microphone is only available to apps in the foreground, so the recording is
            // started from an activity instead of starting the service directly from the tile.
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

    private fun updateTile(isRecording: Boolean = RecorderService.isRunning) {
        val tile = qsTile ?: return
        tile.state = if (isRecording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, org.fossify.commons.R.drawable.ic_microphone_vector)
        tile.label = getString(R.string.app_launcher_name)
        tile.updateTile()
    }
}
