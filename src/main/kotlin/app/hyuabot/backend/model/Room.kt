package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "room")
@Table(name = "room")
data class Room(
    @Id
    @Column(name = "building_name", length = 30)
    val buildingName: String,
    @Id
    @Column(name = "number", length = 30)
    val number: String,
    @Column(name = "name", length = 100)
    val name: String,
)
