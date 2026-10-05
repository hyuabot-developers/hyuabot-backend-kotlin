package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.SubwayTrainDelayID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity(name = "subway_train_delay")
@Table(name = "subway_train_delay")
@IdClass(SubwayTrainDelayID::class)
class SubwayTrainDelay(
    @Id @Column(name = "run_date", nullable = false) val runDate: LocalDate,
    @Id @Column(name = "train_number", length = 10, nullable = false) val trainNumber: String,
    @Column(name = "route_id") var routeID: Int? = null,
    @Column(name = "delay_minutes") var delayMinutes: Int? = null,
    @Column(name = "reference_station_name", length = 30) var referenceStationName: String? = null,
    @Column(name = "updated_at", nullable = false) var updatedAt: ZonedDateTime = ZonedDateTime.now(),
)
