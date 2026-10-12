package com.credstow.app.model

/** Lifecycle status of a key's last (user-initiated) connectivity test. */
enum class KeyStatus {
    UNTESTED,
    TESTING,
    VALID,
    INVALID,
    ERROR;

    companion object {
        fun from(name: String?): KeyStatus = entries.firstOrNull { it.name == name } ?: UNTESTED
    }
}

