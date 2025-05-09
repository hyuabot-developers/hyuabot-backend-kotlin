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
    @Column(name = "route_id", columnDefinition = "integer")
    val id: Int,
    @Column(name = "route_name", length = 30)
    val name: String,
    @Column(name = "route_type_code", length = 10)
    val typeCode: String,
    @Column(name = "route_type_name", length = 10)
    val typeName: String,
    @Column(name = "start_stop_id", columnDefinition = "integer")
    val startStopID: Int,
    @Column(name = "end_stop_id", columnDefinition = "integer")
    val endStopID: Int,
    @Column(name = "up_first_time", columnDefinition = "timetz")
    val upFirstTime: LocalTime,
    @Column(name = "up_last_time", columnDefinition = "timetz")
    val upLastTime: LocalTime,
    @Column(name = "down_first_time", columnDefinition = "timetz")
    val downFirstTime: LocalTime,
    @Column(name = "down_last_time", columnDefinition = "timetz")
    val downLastTime: LocalTime,
    @Column(name = "district_code", columnDefinition = "integer")
    val districtCode: Int,
    @Column(name = "company_id", columnDefinition = "integer")
    val companyID: Int,
    @Column(name = "company_name", length = 30)
    val companyName: String,
    @Column(name = "company_telephone", length = 15)
    val companyPhone: String,
)
