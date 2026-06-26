package dev.shsplayer.features.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Rational

/**
 * SHS Player — Picture-in-Picture helper, backward compatible to Android 7
 * (API 24 / Nougat).
 *
 * Android PiP history:
 *   - API 24-25 (N): Only available on Android TV via `enterPictureInPictureMode()`
 *     (no params). Phones fall back to "background play" via the foreground
 *     service notification.
 *   - API 26 (O): PictureInPictureParams.Builder introduced, phones supported.
 *   - API 31 (S): setAutoEnterEnabled added — system handles entry on Home press.
 *
 * MX Player pattern: graceful fallback — if PiP not available, the video keeps
 * playing in the background via the foreground service notification.
 */
object PipHelper {

    private const val TAG = "PipHelper"

    fun isPipSupported(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.N..Build.VERSION_CODES.N_MR1) {
            val pm = activity.packageManager
            return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val pm = activity.packageManager
        return runCatching {
            pm.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        }.getOrDefault(true)
    }

    fun tryEnterPip(
        activity: Activity,
        videoWidth: Int = 0,
        videoHeight: Int = 0,
        isPlaying: Boolean,
    ): Boolean {
        if (!isPlaying) return false
        if (!isPipSupported(activity)) {
            Log.i(TAG, "PiP not supported on this device — falling back to bg play")
            return false
        }
        return runCatching {
            val aspectRatio = if (videoWidth > 0 && videoHeight > 0) {
                coercePiPRational(Rational(videoWidth, videoHeight))
            } else {
                Rational(16, 9)
            }
            val builder = PictureInPictureParams.Builder().setAspectRatio(aspectRatio)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
                activity.setPictureInPictureParams(builder.build())
                Log.d(TAG, "PiP auto-enter params set (S+)")
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.enterPictureInPictureMode(builder.build())
                Log.d(TAG, "PiP entered (O-R)")
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                @Suppress("DEPRECATION")
                activity.enterPictureInPictureMode()
                Log.d(TAG, "PiP entered (N, TV)")
                true
            } else {
                false
            }
        }.onFailure { e -> Log.w(TAG, "PiP entry failed", e) }.getOrDefault(false)
    }

    private fun coercePiPRational(r: Rational): Rational {
        val max = Rational(239, 100)
        val min = Rational(100, 239)
        return when {
            r > max -> max
            r < min -> min
            else -> r
        }
    }
}
