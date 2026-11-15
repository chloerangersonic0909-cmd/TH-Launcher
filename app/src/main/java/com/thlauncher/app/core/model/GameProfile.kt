package com.thlauncher.app.core.model

/**
 * A named Minecraft installation profile. This is domain data only;
 * the product UI is intentionally owned by the TH Launcher designer.
 */
data class GameProfile(
    val id: String,
    val name: String,
    val gameVersionId: String,
    val javaMajorVersion: Int,
    val minMemoryMb: Int = 512,
    val maxMemoryMb: Int = 2048,
    val gameDirectoryName: String = id
) {
    init {
        require(id.isNotBlank()) { "Profile id must not be blank" }
        require(name.isNotBlank()) { "Profile name must not be blank" }
        require(gameVersionId.isNotBlank()) { "Minecraft version must not be blank" }
        require(javaMajorVersion >= 8) { "Unsupported Java major version" }
        require(minMemoryMb >= 256) { "Minimum memory must be at least 256 MB" }
        require(maxMemoryMb >= minMemoryMb) { "Maximum memory must be >= minimum memory" }
        require(gameDirectoryName.matches(Regex("[A-Za-z0-9._-]+"))) {
            "Game directory name contains unsupported characters"
        }
    }
}
