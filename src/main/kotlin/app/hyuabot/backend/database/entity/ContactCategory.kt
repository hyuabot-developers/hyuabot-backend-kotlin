package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "contact_category")
@Table(name = "phonebook_category")
@SequenceGenerator(name = "phonebook_category_category_id_seq", allocationSize = 1)
data class ContactCategory(
    @Id
    @Column(name = "category_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "phonebook_category_category_id_seq")
    val id: Int? = null,
    @Column(name = "category_name", length = 30, nullable = false)
    val name: String,
    @OneToMany(mappedBy = "category")
    val contact: List<Contact>,
)
