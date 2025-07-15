package app.hyuabot.backend.database.entity

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.Duration

@Entity(name = "subway_route_station")
@Table(name = "subway_route_station")
data class SubwayRouteStation(
    @Id
    @Column(name = "station_id", length = 10, nullable = false)
    val id: String,
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val routeID: Int,
    @Column(name = "station_name", length = 30, nullable = false)
    val name: String,
    @Column(name = "station_sequence", columnDefinition = "integer", nullable = false)
    val order: Int,
    @Type(value = PostgreSQLIntervalType::class)
    @Column(name = "cumulative_time", columnDefinition = "interval", nullable = false)
    val cumulativeTime: Duration,
    @ManyToOne
    @JoinColumn(name = "route_id", referencedColumnName = "route_id", insertable = false, updatable = false)
    val route: SubwayRoute,
    @ManyToOne
    @JoinColumn(name = "station_name", referencedColumnName = "station_name", insertable = false, updatable = false)
    val stationName: SubwayStation,
    @OneToMany(mappedBy = "station")
    val realtime: List<SubwayRealtime>,
    @OneToMany(mappedBy = "station")
    val timetable: List<SubwayTimetable>,
)
