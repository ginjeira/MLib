package app.spotygram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The MLib brand mark shown in the application header. */
class HeaderSticker(@Suppress("UNUSED_PARAMETER") preferences: Preferences) {
    private val image = MutableStateFlow(R.drawable.mlib_mark_vector)
    val drawable = image.asStateFlow()

    /** Kept for compatibility with the existing platform visibility callbacks. */
    fun visibility(@Suppress("UNUSED_PARAMETER") value: Boolean) = Unit
}
