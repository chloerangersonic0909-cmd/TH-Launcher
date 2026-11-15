package com.thlauncher.app.core.storage

import java.io.File

/**
 * Owns TH Launcher's private on-device directory layout.
 * The caller should pass Context.filesDir or Context.getExternalFilesDir(null).
 */
class LauncherDirectories(root: File) {
    val root: File = File(root, "th-launcher")
    val metadata: File = File(this.root, "metadata")
    val runtimes: File = File(this.root, "runtimes")
    val versions: File = File(this.root, "versions")
    val libraries: File = File(this.root, "libraries")
    val assets: File = File(this.root, "assets")
    val profiles: File = File(this.root, "profiles")
    val logs: File = File(this.root, "logs")
    val downloads: File = File(this.root, "downloads")

    fun createRequiredDirectories() {
        listOf(root, metadata, runtimes, versions, libraries, assets, profiles, logs, downloads)
            .forEach { directory ->
                check(directory.exists() || directory.mkdirs()) {
                    "Could not create launcher directory: ${directory.absolutePath}"
                }
            }
    }

    fun versionDirectory(versionId: String): File {
        requireSafeSegment(versionId)
        return File(versions, versionId)
    }

    fun profileDirectory(profileId: String): File {
        requireSafeSegment(profileId)
        return File(profiles, profileId)
    }

    private fun requireSafeSegment(value: String) {
        require(value.matches(Regex("[A-Za-z0-9._-]+"))) {
            "Directory name contains unsupported characters"
        }
    }
}
