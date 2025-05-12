package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "shuttle_route")
@Table(name = "shuttle_route")
data class ShuttleRoute(
    @Id
    @Column(name = "route_name", length = 15, nullable = false)
    val name: String,
    @Column(name = "route_description_korean", length = 100, nullable = false)
    val descriptionKorean: String,
    @Column(name = "route_description_english", length = 100, nullable = false)
    val descriptionEnglish: String,
    @Column(name = "route_tag", length = 10, nullable = false)
    val tag: String,
    @Column(name = "start_stop", length = 15, nullable = false)
    val startStopID: String,
    @Column(name = "end_stop", length = 15, nullable = false)
    val endStopID: String,
)
