package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "reading_room")
@Table(name = "reading_room")
data class ReadingRoom(
    @Id
    @Column(name = "room_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "room_name", length = 20, nullable = false)
    var name: String,
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    var campusID: Int,
    @Column(name = "is_active", columnDefinition = "boolean", nullable = false)
    var isActive: Boolean,
    @Column(name = "is_reservable", columnDefinition = "boolean", nullable = false)
    var isReservable: Boolean,
    @Column(name = "total", columnDefinition = "integer", nullable = false)
    var total: Int,
    @Column(name = "active_total", columnDefinition = "integer", nullable = false)
    var active: Int,
    @Column(name = "occupied", columnDefinition = "integer", nullable = false)
    val occupied: Int,
    @Column(name = "available", columnDefinition = "integer", nullable = false, insertable = false, updatable = false)
    val available: Int?,
    @Column(name = "last_updated_time", columnDefinition = "timestamptz", nullable = false)
    val updatedAt: ZonedDateTime,
    @ManyToOne
    @JoinColumn(name = "campus_id", referencedColumnName = "campus_id", insertable = false, updatable = false)
    val campus: Campus?,
)
