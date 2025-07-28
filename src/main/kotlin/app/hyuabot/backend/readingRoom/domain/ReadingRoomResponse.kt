package app.hyuabot.backend.readingRoom.domain

data class ReadingRoomResponse(
    val seq: Int,
    val name: String,
    val campusID: Int,
    val isActive: Boolean,
    val isReservable: Boolean,
    val total: Int,
    val active: Int,
    val occupied: Int,
    val available: Int,
    val updatedAt: String,
)
