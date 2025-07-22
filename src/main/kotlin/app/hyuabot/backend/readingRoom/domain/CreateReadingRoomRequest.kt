package app.hyuabot.backend.readingRoom.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateReadingRoomRequest(
    @Schema(description = "Campus ID", example = "1")
    val campusID: Int,
    @Schema(description = "Reading Room ID", example = "101")
    val id: Int,
    @Schema(description = "Reading Room Name", example = "Study Room A")
    val name: String,
    @Schema(description = "Total Seats", example = "100")
    val total: Int,
)
