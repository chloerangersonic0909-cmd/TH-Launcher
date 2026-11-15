package com.thlauncher.app.core.input

/**
 * Platform-neutral input configuration for keyboard, mouse, and touch controls.
 * UI and in-game event routing are implemented by their respective modules.
 */
enum class InputDeviceType {
    TOUCH,
    KEYBOARD,
    MOUSE,
    GAMEPAD
}

enum class GameAction {
    MOVE_FORWARD,
    MOVE_BACKWARD,
    MOVE_LEFT,
    MOVE_RIGHT,
    JUMP,
    SNEAK,
    SPRINT,
    INVENTORY,
    DROP_ITEM,
    CHAT,
    ATTACK,
    USE_ITEM,
    PICK_BLOCK,
    PAUSE
}

enum class InputCodeType {
    KEYBOARD_KEY,
    MOUSE_BUTTON,
    TOUCH_CONTROL,
    GAMEPAD_BUTTON,
    GAMEPAD_AXIS
}

/** A stable, serializable identifier for a physical or virtual input. */
data class InputCode(
    val type: InputCodeType,
    val code: String
) {
    init {
        require(code.isNotBlank()) { "Input code cannot be blank" }
        require(code.length <= 64) { "Input code is too long" }
    }
}

/** One action may have multiple bindings, for example keyboard and touch. */
data class ActionBinding(
    val action: GameAction,
    val input: InputCode,
    val device: InputDeviceType
)

/**
 * A user-owned control preset. Keep presets separate by account and Android
 * device so one person's mappings do not overwrite another person's setup.
 */
data class ControlProfile(
    val id: String,
    val ownerAccountId: String?,
    val deviceFingerprint: String?,
    val displayName: String,
    val bindings: List<ActionBinding>,
    val touchControlsEnabled: Boolean = true,
    val mouseEnabled: Boolean = true,
    val keyboardEnabled: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "Control profile id cannot be blank" }
        require(displayName.isNotBlank()) { "Control profile name cannot be blank" }
        require(bindings.distinctBy { it.action to it.input }.size == bindings.size) {
            "Duplicate action/input bindings are not allowed"
        }
    }

    fun bindingsFor(action: GameAction): List<ActionBinding> =
        bindings.filter { it.action == action }

    fun supports(device: InputDeviceType): Boolean = when (device) {
        InputDeviceType.TOUCH -> touchControlsEnabled
        InputDeviceType.MOUSE -> mouseEnabled
        InputDeviceType.KEYBOARD -> keyboardEnabled
        InputDeviceType.GAMEPAD -> bindings.any { it.device == InputDeviceType.GAMEPAD }
    }
}

/** Default Minecraft-style keyboard and mouse bindings; users can customize them. */
object DefaultControlProfile {
    fun create(): ControlProfile = ControlProfile(
        id = "default-java-controls",
        ownerAccountId = null,
        deviceFingerprint = null,
        displayName = "Default Java Controls",
        bindings = listOf(
            key(GameAction.MOVE_FORWARD, "W"),
            key(GameAction.MOVE_BACKWARD, "S"),
            key(GameAction.MOVE_LEFT, "A"),
            key(GameAction.MOVE_RIGHT, "D"),
            key(GameAction.JUMP, "SPACE"),
            key(GameAction.SNEAK, "LEFT_SHIFT"),
            key(GameAction.SPRINT, "LEFT_CONTROL"),
            key(GameAction.INVENTORY, "E"),
            key(GameAction.DROP_ITEM, "Q"),
            key(GameAction.CHAT, "T"),
            key(GameAction.PAUSE, "ESCAPE"),
            mouse(GameAction.ATTACK, "LEFT"),
            mouse(GameAction.USE_ITEM, "RIGHT"),
            mouse(GameAction.PICK_BLOCK, "MIDDLE")
        )
    )

    private fun key(action: GameAction, code: String) = ActionBinding(
        action = action,
        input = InputCode(InputCodeType.KEYBOARD_KEY, code),
        device = InputDeviceType.KEYBOARD
    )

    private fun mouse(action: GameAction, code: String) = ActionBinding(
        action = action,
        input = InputCode(InputCodeType.MOUSE_BUTTON, code),
        device = InputDeviceType.MOUSE
    )
}
