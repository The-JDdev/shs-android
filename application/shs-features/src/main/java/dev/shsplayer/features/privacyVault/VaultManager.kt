package dev.shsplayer.features.privacyVault

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

/**
 * SHS Player — Privacy Vault (clean-room reimplementation inspired by MX Player's
 * PrivateFolder pattern, but with stronger security: secure-erase on delete,
 * lock-on-background, biometric support).
 *
 * Storage: app-private storage at `context.filesDir/vault/{videos,music}/`.
 *   - Files invisible to MediaStore (no scanFile call on vault paths)
 *   - Files deleted on app uninstall
 *   - PIN protected (SHA-256 + salt)
 *
 * MX Player pattern differences (for security parity):
 *   - MX stores files in /sdcard/MxPlayerPro/.private/ with URL-encoded names
 *     (plain, not re-encrypted). We store in app-private storage which is
 *     automatically sandboxed on Android Q+.
 *   - MX uses obfuscated AES for PIN encryption. We use SHA-256 + per-install
 *     salt for simplicity.
 *   - Both implementations support lock-on-background (re-lock when activity
 *     stops, unless playback is active).
 *   - We add secure-erase (overwrite with zeroes) on permanent delete.
 */
private const val PREFS_VAULT = "shs_privacy_vault_prefs"
private const val KEY_PASSWORD_HASH = "password_hash"
private const val KEY_PASSWORD_SALT = "password_salt"
private const val KEY_SECURITY_Q = "security_question"
private const val KEY_SECURITY_A_HASH = "security_answer_hash"
private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
private const val KEY_FILE_META_PREFIX = "vault_files_"

data class VaultFile(
    val id: String,
    val name: String,
    val originalPath: String,
    val type: String, // "video" or "music"
    val size: Long,
    val vaultPath: String,
)

object VaultManager {

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_VAULT, Context.MODE_PRIVATE)

    private fun getSalt(context: Context): String {
        return getPrefs(context).getString(KEY_PASSWORD_SALT, null) ?: run {
            val newSalt = (1..32).map { ('a'..'z').random() }.joinToString()
            getPrefs(context).edit().putString(KEY_PASSWORD_SALT, newSalt).apply()
            newSalt
        }
    }

    private fun sha256(input: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((input + salt).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isVaultSetup(context: Context): Boolean =
        getPrefs(context).contains(KEY_PASSWORD_HASH)

    fun verifyPassword(context: Context, password: String): Boolean {
        val salt = getSalt(context)
        val stored = getPrefs(context).getString(KEY_PASSWORD_HASH, "") ?: ""
        return sha256(password, salt) == stored
    }

    fun setupVault(context: Context, password: String, question: String, answer: String) {
        val salt = getSalt(context)
        getPrefs(context).edit()
            .putString(KEY_PASSWORD_HASH, sha256(password, salt))
            .putString(KEY_SECURITY_Q, question)
            .putString(KEY_SECURITY_A_HASH, sha256(answer.lowercase().trim(), salt))
            .apply()
        getVaultDir(context, "videos").mkdirs()
        getVaultDir(context, "music").mkdirs()
    }

    fun verifySecurityAnswer(context: Context, answer: String): Boolean {
        val salt = getSalt(context)
        val stored = getPrefs(context).getString(KEY_SECURITY_A_HASH, "") ?: ""
        return sha256(answer.lowercase().trim(), salt) == stored
    }

    fun getSecurityQuestion(context: Context): String? =
        getPrefs(context).getString(KEY_SECURITY_Q, null)

    fun isBiometricEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getVaultDir(context: Context, type: String): File =
        File(context.filesDir, "vault/$type")

    fun getVaultFiles(context: Context, type: String): List<VaultFile> {
        val prefs = getPrefs(context)
        val raw = prefs.getStringSet("${KEY_FILE_META_PREFIX}$type", emptySet()) ?: return emptyList()
        return raw.mapNotNull { s ->
            runCatching {
                val parts = s.split("|")
                if (parts.size < 6) return@runCatching null
                VaultFile(parts[0], parts[1], parts[2], parts[3], parts[4].toLong(), parts[5])
            }.getOrNull()
        }
    }

    private fun saveVaultFileMeta(context: Context, type: String, files: List<VaultFile>) {
        val set = files.map { f -> "${f.id}|${f.name}|${f.originalPath}|${f.type}|${f.size}|${f.vaultPath}" }.toSet()
        getPrefs(context).edit().putStringSet("${KEY_FILE_META_PREFIX}$type", set).apply()
    }

    /**
     * Move a MediaStore URI into the vault. The source file is deleted from
     * public storage after a successful copy.
     */
    fun moveToVault(context: Context, uri: Uri, type: String): VaultFile? {
        return runCatching {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (!c.moveToFirst()) return null
                val nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val pathIdx = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "unknown" else "unknown"
                val size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L
                val originalPath = if (pathIdx >= 0) c.getString(pathIdx) ?: "" else ""
                val id = System.currentTimeMillis().toString()
                val vaultDir = getVaultDir(context, type).apply { mkdirs() }
                val destFile = File(vaultDir, "$id-$name")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return null
                runCatching { context.contentResolver.delete(uri, null, null) }
                    .onFailure { Log.w("VaultManager", "delete source failed", it) }
                val vaultFile = VaultFile(id, name, originalPath, type, size, destFile.absolutePath)
                val existing = getVaultFiles(context, type).toMutableList()
                existing.add(vaultFile)
                saveVaultFileMeta(context, type, existing)
                vaultFile
            }
        }.getOrNull()
    }

    /**
     * Restore a vault file back to public storage via MediaStore.
     * Returns true on success.
     */
    fun restoreFromVault(context: Context, vaultFile: VaultFile): Boolean {
        return runCatching {
            val srcFile = File(vaultFile.vaultPath)
            if (!srcFile.exists()) return false
            val ext = vaultFile.name.substringAfterLast('.', "").lowercase()
            val mimeType = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(ext)
                ?: if (vaultFile.type == "video") "video/mp4" else "audio/mpeg"
            val mimeBase = if (vaultFile.type == "video")
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relPath = if (vaultFile.type == "video")
                    android.os.Environment.DIRECTORY_MOVIES + "/SHSPlayer"
                else android.os.Environment.DIRECTORY_MUSIC + "/SHSPlayer"
                val values = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, vaultFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(mimeBase, values) ?: return false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    srcFile.inputStream().use { input -> input.copyTo(out) }
                } ?: run {
                    context.contentResolver.delete(uri, null, null)
                    return false
                }
                val updateValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)
                // Android R+: ask for write access via createWriteRequest
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    runCatching { MediaStore.createWriteRequest(context.contentResolver, listOf(uri)) }
                }
            } else {
                val dir = if (vaultFile.type == "video")
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                else android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
                dir.mkdirs()
                val destFile = File(dir, vaultFile.name)
                srcFile.copyTo(destFile, overwrite = true)
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(destFile.absolutePath), arrayOf(mimeType), null,
                )
            }
            srcFile.delete()
            val remaining = getVaultFiles(context, vaultFile.type).filter { it.id != vaultFile.id }
            saveVaultFileMeta(context, vaultFile.type, remaining)
            true
        }.getOrElse { Log.e("VaultManager", "restore failed", it); false }
    }

    /**
     * Secure-erase a vault file (overwrite with zeroes, then delete).
     */
    fun deleteFromVault(context: Context, vaultFile: VaultFile): Boolean {
        return runCatching {
            val file = File(vaultFile.vaultPath)
            if (file.exists()) {
                // Secure-erase for files < 100MB to avoid ANR
                val len = file.length()
                if (len in 1..(100L * 1024 * 1024)) {
                    try {
                        file.outputStream().use { out ->
                            val buf = ByteArray(8192) { 0 }
                            var written = 0L
                            while (written < len) {
                                val toWrite = minOf(buf.size.toLong(), len - written).toInt()
                                out.write(buf, 0, toWrite)
                                written += toWrite
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("VaultManager", "secure-erase overwrite failed (continuing with delete)", e)
                    }
                }
                file.delete()
            }
            val remaining = getVaultFiles(context, vaultFile.type).filter { it.id != vaultFile.id }
            saveVaultFileMeta(context, vaultFile.type, remaining)
            true
        }.getOrElse { Log.e("VaultManager", "delete failed", it); false }
    }

    /**
     * Play a vault file in the player. Returns the content URI (via FileProvider)
     * that the player should be invoked with.
     *
     * Vault files MUST be played via FileProvider URIs only — never expose the
     * raw file path. The intent is always explicit (ComponentName), never
     * ACTION_VIEW with implicit resolution, to prevent leaking the file to
     * external players.
     */
    fun getPlayableUri(context: Context, vaultFile: VaultFile): Uri {
        val file = File(vaultFile.vaultPath)
        // Use VLC's existing FileProvider authority (${applicationId}.provider)
        // to avoid declaring a separate FileProvider that conflicts with VLC's
        // during manifest merger.
        val authority = context.packageName + ".provider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}

/**
 * Security questions for vault recovery.
 */
val SECURITY_QUESTIONS = listOf(
    "What is your mother's maiden name?",
    "What was the name of your first pet?",
    "What city were you born in?",
    "What was the name of your first school?",
    "What is your oldest sibling's middle name?",
)
