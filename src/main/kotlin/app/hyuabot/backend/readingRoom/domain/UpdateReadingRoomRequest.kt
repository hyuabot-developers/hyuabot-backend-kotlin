package app.hyuabot.backend.readingRoom.domain

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateReadingRoomRequest(
    @Schema(description = "Campus ID", example = "1")
    val campusID: Int,
    @Schema(description = "Reading Room Name", example = "Study Room A")
    val name: String,
    @Schema(description = "Total Seats", example = "100")
    val total: Int,
    @Schema(description = "Active Seats", example = "50")
    val active: Int,
    @Schema(description = "Reading Room Active Status", example = "true")
    val isActive: Boolean,
    @Schema(description = "Reading Room Reservable Status", example = "true")
    val isReservable: Boolean,
)
