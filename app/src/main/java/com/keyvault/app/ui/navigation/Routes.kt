package com.keyvault.app.ui.navigation

/** Single source of truth for compose nav routes. */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LIST = "list"
    const val ADD = "add"
    const val SETTINGS = "settings"
    const val DETAIL_BASE = "detail"
    const val EDIT_BASE = "edit"

    fun detail(id: String) = "$DETAIL_BASE/$id"
    fun edit(id: String) = "$EDIT_BASE/$id"
    const val DETAIL_PATTERN = "detail/{id}"
    const val EDIT_PATTERN = "edit/{id}"
}
