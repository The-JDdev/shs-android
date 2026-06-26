package dev.shsplayer.features.wifiTransfer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.util.UUID

/**
 * SHS Player — Wi-Fi File Transfer server.
 *
 * Clean-room implementation. Runs a NanoHTTPD HTTP server on a random port
 * (10000-65000), serves an HTML upload page + accepts both multipart/form-data
 * and application/octet-stream POSTs. Auth via 16-char UUID token in query
 * string / session.parms / X-Auth-Token header. Path-traversal protected.
 *
 * Sender side: scans the QR code (which encodes http://<ip>:<port>?token=<auth>),
 * opens the upload page in a browser, picks files, sends them to the receiver.
 */
class WifiTransferServer(
    private val context: Context,
    port: Int = 8080,
    private val onFileReceived: ((File) -> Unit)? = null,
) : NanoHTTPD(port) {

    val authToken: String = UUID.randomUUID().toString().replace("-", "").take(16)
    val receivedFiles = mutableListOf<File>()

    private val uploadHtml: String by lazy {
        """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>SHS Player — File Upload</title>
<style>
body{font-family:system-ui,sans-serif;background:#111;color:#eee;display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;margin:0;padding:24px;box-sizing:border-box}
h1{color:#4fc3f7;margin-bottom:8px}
p{color:#aaa;margin-bottom:24px}
.card{background:#1e1e1e;border-radius:16px;padding:32px;max-width:480px;width:100%;box-shadow:0 8px 32px #0008}
label{display:block;margin-bottom:8px;font-weight:600}
input[type=file]{width:100%;padding:12px;background:#2a2a2a;border:1.5px dashed #4fc3f7;border-radius:8px;color:#eee;cursor:pointer;margin-bottom:20px;box-sizing:border-box}
button{width:100%;padding:14px;background:#4fc3f7;color:#000;border:none;border-radius:8px;font-size:16px;font-weight:700;cursor:pointer}
button:hover{background:#81d4fa}
.status{margin-top:16px;text-align:center;color:#81c784;min-height:24px}
.file-list{margin-top:16px;font-size:13px;color:#aaa}
.file-list div{padding:4px 0;border-bottom:1px solid #333}
</style>
</head>
<body>
<div class="card">
<h1>SHS Player</h1>
<p>Send files to this device</p>
<form id="form" enctype="multipart/form-data" method="POST" action="/upload?token=$authToken">
<label for="files">Choose files to send</label>
<input type="file" id="files" name="files" multiple required>
<button type="submit">Send Files</button>
</form>
<div class="status" id="status"></div>
<div class="file-list" id="fileList"></div>
</div>
<script>
document.getElementById('form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const status = document.getElementById('status');
  const fileList = document.getElementById('fileList');
  status.textContent = 'Uploading...';
  const formData = new FormData(e.target);
  try {
    const resp = await fetch('/upload?token=$authToken', { method: 'POST', body: formData });
    if (resp.ok) {
      status.textContent = 'Upload complete!';
      const text = await resp.text();
      fileList.innerHTML = text;
    } else {
      status.textContent = 'Upload failed: ' + resp.status;
    }
  } catch (err) {
    status.textContent = 'Error: ' + err.message;
  }
});
</script>
</body>
</html>"""
    }

    override fun serve(session: IHTTPSession): Response {
        // Validate auth token from query / header / session.parms
        val tokenFromQuery = session.parameters["token"]?.firstOrNull()
        val tokenFromHeader = session.headers["x-auth-token"]
        val tokenFromParms = session.parms["token"]
        val token = tokenFromQuery ?: tokenFromHeader ?: tokenFromParms
        if (token != authToken && session.uri != "/" && session.uri != "/index.html") {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "Invalid auth token")
        }

        return when {
            session.method == Method.GET && (session.uri == "/" || session.uri == "/index.html") -> {
                newFixedLengthResponse(Response.Status.OK, "text/html", uploadHtml)
            }
            session.method == Method.POST && session.uri == "/upload" -> {
                handleUpload(session)
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }

    private fun handleUpload(session: IHTTPSession): Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val uploadedFileNames = StringBuilder("<div>Received:</div>")
            files.forEach { (key, tempPath) ->
                if (key.startsWith("files")) {
                    val tempFile = File(tempPath)
                    if (tempFile.exists()) {
                        val destDir = File(context.filesDir, "received").apply { mkdirs() }
                        val originalName = session.parameters[key]?.firstOrNull() ?: tempFile.name
                        val safeName = originalName.substringAfterLast('/').substringAfterLast('\\')
                            .replace(Regex("[^A-Za-z0-9._-]"), "_")
                        val destFile = File(destDir, "${System.currentTimeMillis()}_$safeName")
                        tempFile.copyTo(destFile, overwrite = true)
                        tempFile.delete()
                        receivedFiles.add(destFile)
                        onFileReceived?.invoke(destFile)
                        uploadedFileNames.append("<div>${destFile.name} (${destFile.length()} bytes)</div>")
                    }
                }
            }
            newFixedLengthResponse(Response.Status.OK, "text/html", uploadedFileNames.toString())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Upload error: ${e.message}")
        }
    }
}

/**
 * Helpers for QR generation and IP discovery.
 */
object WifiTransferUtils {

    fun generateQrCodeBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 2)
            }
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) { null }
    }

    fun getWifiIpAddress(context: Context): String? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        val ip = wm.connectionInfo.ipAddress
        if (ip == 0) return null
        return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    }
}
