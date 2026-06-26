package dev.shsplayer.ui

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import android.util.Log

/**
 * SHS Player — safe MediaStore deletion helper.
 *
 * Original SHS Player code (clean-room reimplementation). Handles the
 * Android version-specific MediaStore deletion flow:
 *   - Android 11+: uses MediaStore.createDeleteRequest (batch + system dialog)
 *   - Android 10: catches RecoverableSecurityException + presents IntentSender
 *   - Android 9 and below: direct contentResolver.delete
 *
 * Caller must register an ActivityResultLauncher<IntentSenderRequest> and
 * pass it via [startIntentSenderForResult].
 */
object MediaDeletionHelper {

    private const val TAG = "MediaDeletionHelper"

    /**
     * Attempt to delete the given media URIs. Returns true if deletion was
     * initiated (or completed synchronously). False if a RecoverableSecurityException
     * was thrown and the caller needs to present the IntentSender.
     */
    fun deleteMediaSafely(
        context: Context,
        uris: List<Uri>,
        startIntentSenderForResult: (IntentSender) -> Unit,
    ): Boolean {
        if (uris.isEmpty()) return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val deleteRequest = MediaStore.createDeleteRequest(context.contentResolver, uris)
                startIntentSenderForResult(deleteRequest.intentSender)
                false  // deletion pending user approval
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                try {
                    uris.forEach { context.contentResolver.delete(it, null, null) }
                    true
                } catch (e: RecoverableSecurityException) {
                    startIntentSenderForResult(e.userAction.actionIntent.intentSender)
                    false
                }
            } else {
                uris.forEach { context.contentResolver.delete(it, null, null) }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteMediaSafely failed", e)
            false
        }
    }

    /**
     * Delete a private file (e.g. received-via-Wi-Fi-Transfer file). No
     * MediaStore involved — just File.delete() with try-catch.
     */
    fun deletePrivateFile(context: Context, filePath: String): Boolean {
        return try {
            java.io.File(filePath).delete()
        } catch (e: Exception) {
            Log.e(TAG, "deletePrivateFile failed", e)
            false
        }
    }
}
