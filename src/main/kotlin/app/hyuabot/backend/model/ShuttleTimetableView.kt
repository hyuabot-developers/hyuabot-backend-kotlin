package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "shuttle_timetable_view")
@Table(name = "shuttle_timetable_grouped_view")
data class ShuttleTimetableView(
    @Id
    @Column(name = "seq", columnDefinition = "integer")
    val seq: Int,
    @Column(name = "period_type", length = 20)
    val periodType: String,
    @Column(name = "weekday")
    val weekday: Boolean,
    @Column(name = "route_name", length = 15)
    val routeName: String,
    @Column(name = "route_tag", length = 10)
    val routeTag: String,
    @Id
    @Column(name = "stop_name", length = 15)
    val stopName: String,
    @Column(name = "departure_time", columnDefinition = "timetz")
    val departureTime: LocalTime,
    @Id
    @Column(name = "destination_group", columnDefinition = "text")
    val destinationGroup: String,
)
