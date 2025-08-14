package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "shuttle_timetable")
@Table(name = "shuttle_timetable")
@SequenceGenerator(name = "shuttle_timetable_seq_seq", allocationSize = 1)
data class ShuttleTimetable(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shuttle_timetable_seq_seq")
    val seq: Int? = null,
    @Column(name = "period_type", length = 20, nullable = false)
    var periodType: String,
    @Column(name = "weekday", nullable = false)
    var weekday: Boolean,
    @Column(name = "route_name", length = 20, nullable = false)
    val routeName: String,
    @Column(name = "departure_time", columnDefinition = "time", nullable = false)
    var departureTime: LocalTime,
    @ManyToOne
    @JoinColumn(name = "route_name", referencedColumnName = "route_name", insertable = false, updatable = false)
    val route: ShuttleRoute?,
)
