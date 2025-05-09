package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "bus_stop")
@Table(name = "bus_stop")
data class BusStop(
    @Id
    @Column(name = "stop_id", columnDefinition = "integer")
    val id: Int,
    @Column(name = "stop_name", length = 30)
    val name: String,
    @Column(name = "district_code", columnDefinition = "integer")
    val districtCode: Int,
    @Column(name = "mobile_number", length = 15)
    val mobileNumber: String,
    @Column(name = "region_name", length = 10)
    val regionName: String,
    @Column(name = "latitude", columnDefinition = "double precision")
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision")
    val longitude: Double,
)
