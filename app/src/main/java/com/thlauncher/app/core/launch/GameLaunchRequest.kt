package com.thlauncher.app.core.launch

import com.thlauncher.app.core.model.GameProfile
import com.thlauncher.app.core.runtime.JavaRuntimeInfo

/**
 * Validated input for the future Android game-process adapter.
 * Actual process launching requires the Android-compatible runtime and renderer integration.
 */
data class GameLaunchRequest(
    val profile: GameProfile,
    val runtime: JavaRuntimeInfo,
    val username: String,
    val accessToken: String,
    val uuid: String,
    val gameArguments: List<String> = emptyList(),
    val jvmArguments: List<String> = emptyList()
) {
    init {
        require(runtime.isUsable) { "Selected Java runtime is not usable" }
        require(username.isNotBlank()) { "Account name is required" }
        require(accessToken.isNotBlank()) { "A valid Minecraft access token is required" }
        require(uuid.isNotBlank()) { "Minecraft profile UUID is required" }
        require(profile.javaMajorVersion == runtime.majorVersion) {
            "Selected runtime does not match the profile Java version"
        }
    }
}
