package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.runtime.Immutable

internal data class AdminThumbnailBackfillScreenUiState(
    val lines: List<Line>,
    val isRunning: Boolean,
    val event: Event,
) {
    internal data class Line(
        val text: String,
        val level: Level,
    )

    internal enum class Level {
        INFO,
        SUCCESS,
        ERROR,
    }

    @Immutable
    interface Event {
        fun onClickStart()

        fun onClickStop()
    }
}
