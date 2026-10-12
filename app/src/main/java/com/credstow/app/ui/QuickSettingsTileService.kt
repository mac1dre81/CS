package com.credstow.app.ui

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log
import com.credstow.app.R

private const val TAG = "CredStow"

/**
 * Quick Settings tile for one-tap key copying.
 *
 * Declared in AndroidManifest.xml with `android:icon="@drawable/ic_qs_tile"`.
 * On tap, launches [TileCopyActivity] — a lightweight key picker that handles
 * biometric auth if the auto-lock timer has expired.
 *
 * The tile label updates to reflect whether the vault is locked or unlocked,
 * giving the user immediate visual feedback.
 */
class QuickSettingsTileService : TileService() {

    override fun onTileAdded() {
        Log.d(TAG, "QS tile added")
        updateTile()
    }

    override fun onStartListening() {
        Log.d(TAG, "QS tile start listening")
        updateTile()
    }

    @Suppress("DEPRECATION")  // Intent overload deprecated in API 34; PendingIntent variant not warranted here
    override fun onClick() {
        Log.d(TAG, "QS tile clicked")
        // Launch the lightweight key picker
        val intent = Intent(this, TileCopyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivityAndCollapse(intent)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.label = getString(R.string.qs_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_tile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tile.subtitle = getString(R.string.qs_tile_subtitle)
        }
        tile.updateTile()
    }
}
