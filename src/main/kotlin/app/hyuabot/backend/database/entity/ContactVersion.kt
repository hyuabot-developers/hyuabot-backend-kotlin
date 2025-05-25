package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "contact_version")
@Table(name = "phonebook_version")
@SequenceGenerator(name = "phonebook_version_version_id_seq", allocationSize = 1)
data class ContactVersion(
    @Id
    @Column(name = "version_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "phonebook_version_version_id_seq")
    val id: Int,
    @Column(name = "version_name", length = 30, nullable = false)
    val name: String,
    @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
    val createdAt: ZonedDateTime,
)
