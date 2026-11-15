package com.thlauncher.app.core.modpack

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ModpackImportResult(
    val destination: File,
    val extractedFiles: Int,
    val totalBytes: Long,
    val detectedManifest: ModpackManifestType
)

enum class ModpackManifestType {
    MODRINTH,
    CURSEFORGE,
    GENERIC_ARCHIVE
}

data class ModpackImportProgress(
    val currentEntry: String,
    val extractedFiles: Int,
    val totalBytes: Long
)

/**
 * Safely imports a user-selected ZIP modpack into an isolated instance folder.
 * This extracts files only; it does not execute mods or claim that dependencies
 * are installed. Manifest-driven dependency resolution is a separate installer step.
 */
class ModpackImportService(
    private val maxEntries: Int = 20_000,
    private val maxUncompressedBytes: Long = 2L * 1024L * 1024L * 1024L
) {
    fun importZip(
        archive: File,
        destination: File,
        shouldCancel: () -> Boolean = { false },
        onProgress: (ModpackImportProgress) -> Unit = {}
    ): ModpackImportResult {
        require(archive.isFile && archive.canRead()) { "Modpack archive cannot be read" }
        require(!destination.exists() || destination.listFiles().isNullOrEmpty()) {
            "Destination must be empty to avoid overwriting existing files"
        }
        check(destination.exists() || destination.mkdirs()) { "Cannot create modpack destination" }

        var files = 0
        var bytes = 0L
        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            while (true) {
                if (shouldCancel()) throw InterruptedException("Modpack import cancelled")
                val entry = zip.nextEntry ?: break
                val relative = entry.name.replace('\\', '/')
                require(isSafeRelativePath(relative)) { "Archive contains an unsafe path" }
                if (entry.isDirectory) {
                    val directory = File(destination, relative)
                    check(directory.canonicalPath.startsWith(destination.canonicalPath + File.separator)) {
                        "Archive directory escapes destination"
                    }
                    check(directory.exists() || directory.mkdirs()) { "Cannot create modpack directory" }
                } else {
                    files++
                    require(files <= maxEntries) { "Modpack contains too many files" }
                    val output = File(destination, relative)
                    check(output.canonicalPath.startsWith(destination.canonicalPath + File.separator)) {
                        "Archive file escapes destination"
                    }
                    output.parentFile?.let { parent ->
                        check(parent.exists() || parent.mkdirs()) { "Cannot create modpack folder" }
                    }
                    FileOutputStream(output).use { sink ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            if (shouldCancel()) throw InterruptedException("Modpack import cancelled")
                            val count = zip.read(buffer)
                            if (count < 0) break
                            bytes += count
                            require(bytes <= maxUncompressedBytes) { "Modpack expands beyond the size limit" }
                            sink.write(buffer, 0, count)
                        }
                    }
                    onProgress(ModpackImportProgress(relative, files, bytes))
                }
                zip.closeEntry()
            }
        }
        val manifest = when {
            File(destination, "modrinth.index.json").isFile -> ModpackManifestType.MODRINTH
            File(destination, "manifest.json").isFile -> ModpackManifestType.CURSEFORGE
            else -> ModpackManifestType.GENERIC_ARCHIVE
        }
        return ModpackImportResult(destination, files, bytes, manifest)
    }

    private fun isSafeRelativePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith('/') || path.startsWith("../")) return false
        if (path.split('/').any { it == ".." || it.isBlank() }) return false
        return !Regex("^[A-Za-z]:").containsMatchIn(path)
    }
}
