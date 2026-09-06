package com.sahin.smfinddevice.data

import kotlinx.serialization.Serializable

/** Exactly 15 command slots are ever allowed to exist (see CommandRepository). */
const val MAX_RECOVERY_COMMAND_SLOTS = 15

@Serializable
data class RecoveryCommand(
    val slot: Int,
    val text: String,
    val enabled: Boolean = true
) {
    companion object {
        /** Normalize for exact, non-fuzzy matching: trim + collapse inner whitespace + uppercase. */
        fun normalize(raw: String): String =
            raw.trim().replace(Regex("\\s+"), " ").uppercase()
    }

    val normalized: String get() = normalize(text)
}

fun defaultRecoveryCommands(): List<RecoveryCommand> = listOf(
    "SAHIN",
    "FIND SAHIN",
    "SEND LOCATION",
    "FIND MY DEVICE",
    "SHARE LOCATION",
    "HELLO SAHIN",
    "PLEASE SAHIN",
    "GIVE ME EXACT LOCATION",
    "SHARE",
    "LOCATE SAHIN",
    "FIND PHONE",
    "WHERE ARE YOU",
    "MY PHONE",
    "TRACK DEVICE",
    "SM FIND"
).mapIndexed { index, text -> RecoveryCommand(slot = index, text = text, enabled = true) }
