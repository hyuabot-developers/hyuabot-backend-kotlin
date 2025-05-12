package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "bus_route")
@Table(name = "bus_route")
data class BusRoute(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "route_name", length = 30, nullable = false)
    val name: String,
    @Column(name = "route_type_code", length = 10, nullable = false)
    val typeCode: String,
    @Column(name = "route_type_name", length = 10, nullable = false)
    val typeName: String,
    @Column(name = "start_stop_id", columnDefinition = "integer", nullable = false)
    val startStopID: Int,
    @Column(name = "end_stop_id", columnDefinition = "integer", nullable = false)
    val endStopID: Int,
    @Column(name = "up_first_time", columnDefinition = "timetz", nullable = false)
    val upFirstTime: LocalTime,
    @Column(name = "up_last_time", columnDefinition = "timetz", nullable = false)
    val upLastTime: LocalTime,
    @Column(name = "down_first_time", columnDefinition = "timetz", nullable = false)
    val downFirstTime: LocalTime,
    @Column(name = "down_last_time", columnDefinition = "timetz", nullable = false)
    val downLastTime: LocalTime,
    @Column(name = "district_code", columnDefinition = "integer", nullable = false)
    val districtCode: Int,
    @Column(name = "company_id", columnDefinition = "integer", nullable = false)
    val companyID: Int,
    @Column(name = "company_name", length = 30, nullable = false)
    val companyName: String,
    @Column(name = "company_telephone", length = 15, nullable = false)
    val companyPhone: String,
)
