package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "subway_station")
@Table(name = "subway_station")
data class SubwayStation(
    @Id
    @Column(name = "station_name", length = 30, nullable = false)
    val name: String,
    @OneToMany(mappedBy = "stationName")
    val subwayLine: List<SubwayRouteStation> = emptyList(),
)
