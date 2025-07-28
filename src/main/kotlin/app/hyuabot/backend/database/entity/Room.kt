package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "room")
@Table(
    name = "room",
    indexes = [
        Index(name = "idx_room_building_name", columnList = "building_name, number", unique = false),
    ],
)
@SequenceGenerator(name = "room_seq_seq", allocationSize = 1)
data class Room(
    @Id
    @Column(name = "seq", columnDefinition = "serial", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_seq_seq")
    val seq: Int? = null,
    @Column(name = "building_name", length = 30, nullable = false)
    var buildingName: String,
    @Column(name = "number", length = 30, nullable = false)
    var number: String,
    @Column(name = "name", length = 100, nullable = false)
    var name: String,
    @ManyToOne
    @JoinColumn(name = "building_name", referencedColumnName = "name", insertable = false, updatable = false)
    val building: Building? = null,
)
