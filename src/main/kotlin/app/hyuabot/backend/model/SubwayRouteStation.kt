package app.hyuabot.backend.model

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import kotlin.time.Duration

@Entity(name = "subway_route_station")
@Table(name = "subway_route_station")
data class SubwayRouteStation(
    @Id
    @Column(name = "station_id", length = 10)
    val id: String,
    @Column(name = "route_id", columnDefinition = "integer")
    val routeID: Int,
    @Column(name = "station_name", length = 30)
    val name: String,
    @Column(name = "station_sequence", columnDefinition = "integer")
    val order: Int,
    @Type(value = PostgreSQLIntervalType::class)
    @Column(name = "cumulative_time", columnDefinition = "interval")
    val cumulativeTime: Duration,
)
