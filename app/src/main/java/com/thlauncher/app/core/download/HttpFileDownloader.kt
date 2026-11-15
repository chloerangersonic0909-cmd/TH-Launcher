package com.thlauncher.app.core.download

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val bytesPerSecond: Long
) {
    val fraction: Float?
        get() = totalBytes?.takeIf { it > 0L }?.let {
            (downloadedBytes.toFloat() / it).coerceIn(0f, 1f)
        }
}

data class DownloadRequest(
    val url: String,
    val destination: File,
    val expectedSha1: String? = null,
    val expectedSha256: String? = null,
    val connectTimeoutMs: Int = 15_000,
    val readTimeoutMs: Int = 30_000
)

class DownloadCancelledException : InterruptedException("Download cancelled")

/**
 * Streaming HTTP downloader with Range resume, cancellation, progress and
 * optional SHA-1/SHA-256 verification. Run on a worker thread.
 */
class HttpFileDownloader {
    fun download(
        request: DownloadRequest,
        shouldCancel: () -> Boolean = { false },
        onProgress: (DownloadProgress) -> Unit = {}
    ): File {
        require(request.url.startsWith("https://")) { "Only HTTPS downloads are allowed" }
        require(request.expectedSha1 == null || request.expectedSha1.matches(Regex("[a-fA-F0-9]{40}")))
        require(request.expectedSha256 == null || request.expectedSha256.matches(Regex("[a-fA-F0-9]{64}")))

        val destination = request.destination.absoluteFile
        val parent = destination.parentFile
            ?: throw IllegalArgumentException("Destination must have a parent directory")
        check(parent.exists() || parent.mkdirs()) { "Cannot create download directory" }
        val partial = File(parent, destination.name + ".part")
        var connection: HttpURLConnection? = null
        try {
            val existing = if (partial.exists()) partial.length() else 0L
            connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = request.connectTimeoutMs
                readTimeout = request.readTimeoutMs
                setRequestProperty("User-Agent", "TH-Launcher/0.1.0")
                if (existing > 0L) setRequestProperty("Range", "bytes=$existing-")
            }
            val status = connection.responseCode
            if (existing > 0L && status == HttpURLConnection.HTTP_OK) {
                partial.delete()
            } else if (status !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) {
                throw IllegalStateException("Download failed: HTTP $status")
            }
            val append = existing > 0L && status == HttpURLConnection.HTTP_PARTIAL
            val startingBytes = if (append) existing else 0L
            val responseLength = connection.getHeaderFieldLong("Content-Length", -1L)
            val total = if (responseLength >= 0L) {
                responseLength + startingBytes
            } else {
                connection.getHeaderField("Content-Range")
                    ?.substringAfterLast('/')?.toLongOrNull()
            }
            var downloaded = startingBytes
            var lastBytes = downloaded
            var lastTime = System.nanoTime()
            connection.inputStream.use { input ->
                RandomAccessFile(partial, "rw").use { output ->
                    if (append) output.seek(existing) else output.setLength(0L)
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (shouldCancel() || Thread.currentThread().isInterrupted) {
                            throw DownloadCancelledException()
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        val now = System.nanoTime()
                        if (now - lastTime >= 500_000_000L) {
                            val speed = ((downloaded - lastBytes) * 1_000_000_000L /
                                (now - lastTime).coerceAtLeast(1L))
                            onProgress(DownloadProgress(downloaded, total, speed))
                            lastBytes = downloaded
                            lastTime = now
                        }
                    }
                }
            }
            onProgress(DownloadProgress(downloaded, total, 0L))
            request.expectedSha1?.let { expected ->
                check(digest(partial, "SHA-1").equals(expected, ignoreCase = true)) {
                    "SHA-1 verification failed"
                }
            }
            request.expectedSha256?.let { expected ->
                check(digest(partial, "SHA-256").equals(expected, ignoreCase = true)) {
                    "SHA-256 verification failed"
                }
            }
            if (destination.exists()) check(destination.delete()) { "Cannot replace existing destination" }
            check(partial.renameTo(destination)) { "Cannot finalize downloaded file" }
            return destination
        } catch (error: Throwable) {
            // Keep partial file for a possible resume, including after cancellation.
            throw error
        } finally {
            connection?.disconnect()
        }
    }

    private fun digest(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
    }
}
