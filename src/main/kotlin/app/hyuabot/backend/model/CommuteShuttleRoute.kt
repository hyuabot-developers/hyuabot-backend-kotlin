package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "commute_shuttle_route")
@Table(name = "commute_shuttle_route")
data class CommuteShuttleRoute(
    @Id
    @Column(name = "route_name", length = 15)
    val name: String,
    @Column(name = "route_description_korean", length = 100)
    val descriptionKorean: String,
    @Column(name = "route_description_english", length = 100)
    val descriptionEnglish: String,
)
