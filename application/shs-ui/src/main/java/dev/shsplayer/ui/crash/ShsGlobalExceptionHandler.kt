package dev.shsplayer.ui.crash

import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * SHS Player — global uncaught exception handler.
 *
 * Original SHS Player code (clean-room reimplementation). Installed as the
 * default uncaught exception handler. Captures the exception stack trace,
 * launches [ShsCrashActivity] with the trace + logcat output, then exits.
 */
class ShsGlobalExceptionHandler(
    private val context: android.content.Context,
    private val crashActivityClass: Class<*>,
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val stackTrace = Log.getStackTraceString(e)
            val logcat = captureLogcat()
            val intent = android.content.Intent(context, crashActivityClass).apply {
                putExtra(EXTRA_EXCEPTION, stackTrace)
                putExtra(EXTRA_LOGCAT, logcat)
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK,
                )
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            // If crash reporter itself crashes, fall back to default handler
        } finally {
            defaultHandler?.uncaughtException(t, e)
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun captureLogcat(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "500"))
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Failed to capture logcat: ${e.message}"
        }
    }

    companion object {
        const val EXTRA_EXCEPTION = "shs_crash_exception"
        const val EXTRA_LOGCAT = "shs_crash_logcat"
    }
}
