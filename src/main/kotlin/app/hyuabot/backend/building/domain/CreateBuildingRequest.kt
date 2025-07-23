package app.hyuabot.backend.building.domain

data class CreateBuildingRequest(
    val id: String?,
    val name: String,
    val campusID: Int,
    val latitude: Double,
    val longitude: Double,
    val url: String,
)
