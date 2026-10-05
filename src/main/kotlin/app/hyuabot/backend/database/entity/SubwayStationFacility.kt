package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "subway_station_facility")
@Table(name = "subway_station_facility")
@SequenceGenerator(name = "subway_station_facility_seq_seq", allocationSize = 1)
class SubwayStationFacility(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subway_station_facility_seq_seq")
    val seq: Int? = null,
    @Column(name = "station_id", length = 10, nullable = false)
    var stationID: String,
    @Column(name = "facility_type", length = 20, nullable = false)
    var facilityType: String,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,
    @Column(name = "exit_no", length = 10)
    var exitNumber: String? = null,
    @Column(name = "from_place", length = 50)
    var fromPlace: String? = null,
    @Column(name = "to_place", length = 50)
    var toPlace: String? = null,
    @Column(name = "description_korean", length = 200)
    var descriptionKorean: String? = null,
    @Column(name = "description_english", length = 200)
    var descriptionEnglish: String? = null,
    @Column(name = "source", length = 30, nullable = false)
    var source: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SubwayStationFacility
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
