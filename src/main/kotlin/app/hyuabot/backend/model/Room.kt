package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity(name = "room")
@Table(name = "room")
data class Room(
    @Id
    @Column(name = "building_name", length = 30, nullable = false)
    val buildingName: String,
    @Id
    @Column(name = "number", length = 30, nullable = false)
    val number: String,
    @Column(name = "name", length = 100, nullable = false)
    val name: String,
    @ManyToOne
    val building: Building,
)
