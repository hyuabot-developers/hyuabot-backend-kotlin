package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "bus_route")
@Table(name = "bus_route")
class BusRoute(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "route_name", length = 30, nullable = false)
    var name: String,
    @Column(name = "route_type_code", length = 10, nullable = false)
    var typeCode: String,
    @Column(name = "route_type_name", length = 10, nullable = false)
    var typeName: String,
    @Column(name = "start_stop_id", columnDefinition = "integer", nullable = false)
    var startStopID: Int,
    @Column(name = "end_stop_id", columnDefinition = "integer", nullable = false)
    var endStopID: Int,
    @Column(name = "up_first_time", columnDefinition = "time", nullable = false)
    var upFirstTime: LocalTime,
    @Column(name = "up_last_time", columnDefinition = "time", nullable = false)
    var upLastTime: LocalTime,
    @Column(name = "down_first_time", columnDefinition = "time", nullable = false)
    var downFirstTime: LocalTime,
    @Column(name = "down_last_time", columnDefinition = "time", nullable = false)
    var downLastTime: LocalTime,
    @Column(name = "district_code", columnDefinition = "integer", nullable = false)
    var districtCode: Int,
    @Column(name = "company_id", columnDefinition = "integer", nullable = false)
    var companyID: Int,
    @Column(name = "company_name", length = 30, nullable = false)
    var companyName: String,
    @Column(name = "company_telephone", length = 15, nullable = false)
    var companyPhone: String,
    @OneToMany(mappedBy = "route")
    val stop: MutableList<BusRouteStop>,
    @OneToOne
    @JoinColumn(name = "start_stop_id", insertable = false, updatable = false)
    val startStop: BusStop,
    @OneToOne
    @JoinColumn(name = "end_stop_id", insertable = false, updatable = false)
    val endStop: BusStop,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as BusRoute
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
