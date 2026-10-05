package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "subway_alert")
@Table(name = "subway_alert")
class SubwayAlert(
    @Id @Column(name = "alert_id", length = 50, nullable = false) val id: String,
    @Column(name = "route_id") var routeID: Int? = null,
    @Column(name = "title", length = 200, nullable = false) var title: String,
    @Column(name = "content") var content: String? = null,
    @Column(name = "starts_at") var startsAt: ZonedDateTime? = null,
    @Column(name = "ends_at") var endsAt: ZonedDateTime? = null,
    @Column(name = "source", length = 30, nullable = false) var source: String = "SEOUL_METRO",
    @Column(name = "updated_at", nullable = false) var updatedAt: ZonedDateTime = ZonedDateTime.now(),
)
