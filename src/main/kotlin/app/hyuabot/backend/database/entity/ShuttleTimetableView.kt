package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.ShuttleTimetableViewID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "shuttle_timetable_view")
@Table(name = "shuttle_timetable_grouped_view")
@IdClass(ShuttleTimetableViewID::class)
data class ShuttleTimetableView(
    @Id
    @Column(name = "seq", columnDefinition = "integer", nullable = false)
    val seq: Int,
    @Column(name = "period_type", length = 20, nullable = false)
    val periodType: String,
    @Column(name = "weekday", nullable = false)
    val weekday: Boolean,
    @Column(name = "route_name", length = 15, nullable = false)
    val routeName: String,
    @Column(name = "route_tag", length = 10, nullable = false)
    val routeTag: String,
    @Id
    @Column(name = "stop_name", length = 15, nullable = false)
    val stopName: String,
    @Column(name = "departure_time", columnDefinition = "timetz", nullable = false)
    val departureTime: LocalTime,
    @Id
    @Column(name = "destination_group", columnDefinition = "text", nullable = false)
    val destinationGroup: String,
)
