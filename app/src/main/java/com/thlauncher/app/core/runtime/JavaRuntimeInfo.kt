package com.thlauncher.app.core.runtime

import java.io.File

enum class RuntimeArchitecture { ARM64, ARM32, X86_64, X86, UNKNOWN }

data class JavaRuntimeInfo(
    val id: String,
    val majorVersion: Int,
    val architecture: RuntimeArchitecture,
    val homeDirectory: File,
    val javaExecutable: File
) {
    val isUsable: Boolean
        get() = majorVersion >= 8 &&
            homeDirectory.isDirectory &&
            javaExecutable.isFile &&
            javaExecutable.canExecute()
}
