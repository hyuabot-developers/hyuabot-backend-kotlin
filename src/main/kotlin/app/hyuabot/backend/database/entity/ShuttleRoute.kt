package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "shuttle_route")
@Table(name = "shuttle_route")
data class ShuttleRoute(
    @Id
    @Column(name = "route_name", length = 15, nullable = false)
    val name: String,
    @Column(name = "route_description_korean", length = 100, nullable = false)
    var descriptionKorean: String,
    @Column(name = "route_description_english", length = 100, nullable = false)
    var descriptionEnglish: String,
    @Column(name = "route_tag", length = 10, nullable = false)
    var tag: String,
    @Column(name = "start_stop", length = 15, nullable = false)
    var startStopID: String,
    @Column(name = "end_stop", length = 15, nullable = false)
    var endStopID: String,
    @OneToMany(mappedBy = "route")
    val timetable: List<ShuttleTimetable> = emptyList(),
    @OneToMany(mappedBy = "route")
    val stop: List<ShuttleRouteStop> = emptyList(),
    @ManyToOne
    @JoinColumn(name = "start_stop", referencedColumnName = "stop_name", insertable = false, updatable = false)
    val startStop: ShuttleStop?,
    @ManyToOne
    @JoinColumn(name = "end_stop", referencedColumnName = "stop_name", insertable = false, updatable = false)
    val endStop: ShuttleStop?,
)
