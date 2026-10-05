package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.BusLocationID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.ZonedDateTime

@Entity(name = "bus_location")
@Table(name = "bus_location")
@IdClass(BusLocationID::class)
class BusLocation(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    var routeID: Int,
    @Id
    @Column(name = "plate_no", length = 20, nullable = false)
    var plateNumber: String,
    @Column(name = "station_seq", nullable = false)
    var stationSeq: Int,
    @Column(name = "station_id")
    var stationID: Int? = null,
    @Column(name = "crowded", columnDefinition = "smallint")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    var crowded: Int? = null,
    @Column(name = "remaining_seat_count")
    var remainingSeatCount: Int? = null,
    @Column(name = "low_plate")
    var lowFloor: Boolean? = null,
    @Column(name = "state_code", columnDefinition = "smallint")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    var stateCode: Int? = null,
    @Column(name = "last_updated_time", columnDefinition = "timestamptz", nullable = false)
    var updatedAt: ZonedDateTime,
)
