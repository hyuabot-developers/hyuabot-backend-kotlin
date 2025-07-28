package app.hyuabot.backend.building.domain

data class UpdateBuildingRequest(
    val id: String?,
    val campusID: Int,
    val latitude: Double,
    val longitude: Double,
    val url: String,
)
