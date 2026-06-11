package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "commute_shuttle_stop")
@Table(name = "commute_shuttle_stop")
class CommuteShuttleStop(
    @Id
    @Column(name = "stop_name", length = 50, nullable = false)
    val name: String,
    @Column(name = "description", length = 100, nullable = false)
    var description: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    var longitude: Double,
    @OneToMany(mappedBy = "stop")
    val timetable: MutableList<CommuteShuttleTimetable>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as CommuteShuttleStop
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
