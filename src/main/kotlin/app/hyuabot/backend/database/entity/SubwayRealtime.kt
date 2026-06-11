package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.SubwayRealtimeID
import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Type
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Objects

@Entity(name = "subway_realtime")
@Table(name = "subway_realtime")
@IdClass(SubwayRealtimeID::class)
class SubwayRealtime(
    @Id
    @Column(name = "station_id", length = 10, nullable = false)
    val stationID: String,
    @Id
    @Column(name = "up_down_type", length = 10, nullable = false)
    val heading: String,
    @Id
    @Column(name = "arrival_seq", columnDefinition = "integer", nullable = false)
    val order: Int,
    @Column(name = "current_station_name", length = 30)
    var location: String,
    @Column(name = "remaining_stop_count", columnDefinition = "integer", nullable = false)
    var remainingStop: Int,
    @Type(value = PostgreSQLIntervalType::class)
    @Column(name = "remaining_time", columnDefinition = "interval", nullable = false)
    var remainingTime: Duration,
    @Column(name = "terminal_station_id", length = 10, nullable = false)
    var terminalStationID: String,
    @Column(name = "train_number", length = 10, nullable = false)
    var trainNumber: String,
    @Column(name = "last_updated_time", columnDefinition = "timestamptz", nullable = false)
    var updatedAt: ZonedDateTime,
    @Column(name = "is_express_train", columnDefinition = "boolean", nullable = false)
    var isExpress: Boolean,
    @Column(name = "is_last_train", columnDefinition = "boolean", nullable = false)
    var isLast: Boolean,
    @Column(name = "status_code", columnDefinition = "integer", nullable = false)
    var status: Int,
    @ManyToOne
    @JoinColumn(name = "station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    val station: SubwayRouteStation?,
    @OneToOne
    @JoinColumn(name = "terminal_station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    val terminalStation: SubwayRouteStation?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SubwayRealtime
        return stationID == other.stationID &&
            heading == other.heading &&
            order == other.order
    }

    override fun hashCode(): Int = Objects.hash(stationID, heading, order)
}
