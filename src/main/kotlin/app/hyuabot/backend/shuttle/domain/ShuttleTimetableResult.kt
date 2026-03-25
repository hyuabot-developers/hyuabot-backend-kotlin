package app.hyuabot.backend.shuttle.domain

data class ShuttleTimetableResult(
    val order: List<ShuttleTimetableViewItem>,
    val destination: Map<String, List<ShuttleTimetableViewItem>>,
)
