package com.thlauncher.app.core

/**
 * Domain state for the launcher. Keep Android UI concerns out of this package.
 */
sealed interface LauncherState {
    data object Idle : LauncherState
    data object PreparingRuntime : LauncherState
    data object InstallingGame : LauncherState
    data object Ready : LauncherState
    data class Failed(val reason: String) : LauncherState
}
