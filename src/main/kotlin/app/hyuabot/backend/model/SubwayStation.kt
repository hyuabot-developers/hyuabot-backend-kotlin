package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "subway_station")
@Table(name = "subway_station")
data class SubwayStation(
    @Id
    @Column(name = "station_name", length = 30)
    val name: String,
)
