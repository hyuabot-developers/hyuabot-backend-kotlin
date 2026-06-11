package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "commute_shuttle_route")
@Table(name = "commute_shuttle_route")
class CommuteShuttleRoute(
    @Id
    @Column(name = "route_name", length = 15, nullable = false)
    val name: String,
    @Column(name = "route_description_korean", length = 100, nullable = false)
    var descriptionKorean: String,
    @Column(name = "route_description_english", length = 100, nullable = false)
    var descriptionEnglish: String,
    @OneToMany(mappedBy = "route")
    val timetable: MutableList<CommuteShuttleTimetable>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as CommuteShuttleRoute
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
