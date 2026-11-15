package com.thlauncher.app.core.model

enum class MinecraftVersionType { RELEASE, SNAPSHOT, OLD_BETA, OLD_ALPHA }

data class MinecraftVersion(
    val id: String,
    val type: MinecraftVersionType,
    val url: String,
    val releaseTime: String,
    val javaMajorVersion: Int?
)
