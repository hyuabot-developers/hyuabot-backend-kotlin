package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "contact")
@Table(name = "phonebook")
@SequenceGenerator(name = "phonebook_phonebook_id_seq", allocationSize = 1)
data class Contact(
    @Id
    @Column(name = "phonebook_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "phonebook_phonebook_id_seq")
    val id: Int,
    @Column(name = "campus_id", columnDefinition = "integer")
    val campusID: Int,
    @Column(name = "category_id", columnDefinition = "integer")
    val categoryID: Int,
    @Column(name = "name", columnDefinition = "text")
    val name: String,
    @Column(name = "phone", length = 30)
    val phone: String,
)
