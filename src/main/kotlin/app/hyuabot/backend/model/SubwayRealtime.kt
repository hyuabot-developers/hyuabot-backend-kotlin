package app.hyuabot.backend.model

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.ZonedDateTime
import kotlin.time.Duration

@Entity(name = "subway_realtime")
@Table(name = "subway_realtime")
data class SubwayRealtime(
    @Id
    @Column(name = "station_id", length = 10)
    val stationID: String,
    @Id
    @Column(name = "up_down_type", length = 10)
    val heading: String,
    @Id
    @Column(name = "arrival_sequence", columnDefinition = "integer")
    val order: Int,
    @Column(name = "current_station_name", length = 30)
    val location: String,
    @Column(name = "remaining_stop_count", columnDefinition = "integer")
    val remainingStop: Int,
    @Type(value = PostgreSQLIntervalType::class)
    @Column(name = "remaining_time", columnDefinition = "interval")
    val remainingTime: Duration,
    @Column(name = "terminal_station_id", length = 10)
    val terminalStationID: String,
    @Column(name = "train_number", length = 10)
    val trainNumber: String,
    @Column(name = "last_updated_time", columnDefinition = "timestamptz")
    val updatedAt: ZonedDateTime,
    @Column(name = "is_express_train", columnDefinition = "boolean")
    val isExpress: Boolean,
    @Column(name = "is_last_train", columnDefinition = "boolean")
    val isLast: Boolean,
    @Column(name = "status_code", columnDefinition = "integer")
    val status: Int,
)
