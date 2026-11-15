package com.thlauncher.app.core.modpack

import com.thlauncher.app.core.download.DownloadProgress
import com.thlauncher.app.core.download.DownloadRequest
import com.thlauncher.app.core.download.HttpFileDownloader
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

data class ModrinthPackProgress(
    val stage: String,
    val completedFiles: Int,
    val totalFiles: Int,
    val currentPath: String,
    val downloadProgress: DownloadProgress? = null
)

data class ModrinthPackInstallResult(
    val instanceDirectory: File,
    val minecraftVersion: String,
    val loaders: List<String>,
    val installedFiles: Int
)

/**
 * Installs Modrinth .mrpack files: copies overrides and downloads manifest files
 * with SHA-1 verification. Call from a background worker.
 *
 * This intentionally supports the Modrinth format only. CurseForge manifests
 * require their supported download/API flow and are not silently treated as installed.
 */
class ModrinthPackInstaller(
    private val downloader: HttpFileDownloader = HttpFileDownloader()
) {
    fun install(
        archive: File,
        instanceDirectory: File,
        shouldCancel: () -> Boolean = { false },
        onProgress: (ModrinthPackProgress) -> Unit = {}
    ): ModrinthPackInstallResult {
        require(archive.isFile && archive.canRead()) { "Modpack archive cannot be read" }
        require(!instanceDirectory.exists() || instanceDirectory.listFiles().isNullOrEmpty()) {
            "Choose an empty instance directory"
        }
        check(instanceDirectory.exists() || instanceDirectory.mkdirs()) {
            "Cannot create instance directory"
        }

        ZipFile(archive).use { zip ->
            val manifestEntry = zip.getEntry("modrinth.index.json")
                ?: throw IllegalArgumentException("This is not a supported Modrinth .mrpack file")
            val manifest = zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
            val root = JSONObject(manifest)
            require(root.optInt("formatVersion", -1) == 1) { "Unsupported Modrinth pack format" }
            val gameVersion = root.getJSONObject("dependencies").getString("minecraft")
            val loadersJson = root.getJSONObject("dependencies")
            val loaders = listOf("fabric", "forge", "neoforge", "quilt")
                .filter { loadersJson.has(it) }
                .map { "$it:${loadersJson.getString(it)}" }

            val overrides = root.optString("overrides", "overrides")
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                if (shouldCancel()) throw InterruptedException("Modpack installation cancelled")
                val entry = entries.nextElement()
                if (!entry.isDirectory && entry.name.startsWith("$overrides/")) {
                    val relative = entry.name.removePrefix("$overrides/")
                    val output = safeTarget(instanceDirectory, relative)
                    output.parentFile?.let {
                        check(it.exists() || it.mkdirs()) { "Cannot create override directory" }
                    }
                    zip.getInputStream(entry).use { input ->
                        output.outputStream().use { outputStream -> input.copyTo(outputStream) }
                    }
                }
            }

            val files = root.getJSONArray("files")
            val total = files.length()
            var completed = 0
            for (index in 0 until total) {
                if (shouldCancel()) throw InterruptedException("Modpack installation cancelled")
                val item = files.getJSONObject(index)
                val path = item.getString("path")
                val target = safeTarget(instanceDirectory, path)
                val hashes = item.getJSONObject("hashes")
                val sha1 = hashes.optString("sha1").takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("Modpack file is missing a SHA-1 hash: $path")
                val downloads = item.getJSONArray("downloads")
                require(downloads.length() > 0) { "No download URL for modpack file: $path" }
                val url = downloads.getString(0)
                target.parentFile?.let {
                    check(it.exists() || it.mkdirs()) { "Cannot create mod directory" }
                }
                onProgress(ModrinthPackProgress("downloading", completed, total, path))
                downloader.download(
                    DownloadRequest(url = url, destination = target, expectedSha1 = sha1),
                    shouldCancel = shouldCancel,
                    onProgress = { progress ->
                        onProgress(ModrinthPackProgress("downloading", completed, total, path, progress))
                    }
                )
                completed++
                onProgress(ModrinthPackProgress("installing", completed, total, path))
            }

            return ModrinthPackInstallResult(instanceDirectory, gameVersion, loaders, completed)
        }
    }

    private fun safeTarget(root: File, relativePath: String): File {
        val normalized = relativePath.replace('\\', '/')
        require(normalized.isNotBlank() && !normalized.startsWith('/'))
        require(normalized.split('/').none { it == ".." || it.isBlank() })
        val target = File(root, normalized)
        check(target.canonicalPath.startsWith(root.canonicalPath + File.separator)) {
            "Modpack path escapes instance directory"
        }
        return target
    }
}
