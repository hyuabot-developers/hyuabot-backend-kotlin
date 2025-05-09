package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "reading_room")
@Table(name = "reading_room")
data class ReadingRoom(
    @Id
    @Column(name = "room_id", columnDefinition = "integer")
    val id: Int,
    @Column(name = "room_name", length = 20)
    val name: String,
    @Column(name = "campus_id", columnDefinition = "integer")
    val campusID: Int,
    @Column(name = "is_active", columnDefinition = "boolean")
    val isActive: Boolean,
    @Column(name = "is_reservable", columnDefinition = "boolean")
    val isReservable: Boolean,
    @Column(name = "total", columnDefinition = "integer")
    val total: Int,
    @Column(name = "active_total", columnDefinition = "integer")
    val active: Int,
    @Column(name = "occupied", columnDefinition = "integer")
    val occupied: Int,
    @Column(name = "available", columnDefinition = "integer")
    val available: Int,
    @Column(name = "last_updated_time", columnDefinition = "timestamptz")
    val updatedAt: ZonedDateTime,
)
