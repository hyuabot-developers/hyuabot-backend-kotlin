package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "contact")
@Table(name = "phonebook")
@SequenceGenerator(name = "phonebook_phonebook_id_seq", allocationSize = 1)
class Contact(
    @Id
    @Column(name = "phonebook_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "phonebook_phonebook_id_seq")
    val id: Int? = null,
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    var campusID: Int,
    @Column(name = "category_id", columnDefinition = "integer", nullable = false)
    var categoryID: Int,
    @Column(name = "name", columnDefinition = "text", nullable = false)
    var name: String,
    @Column(name = "phone", length = 30, nullable = false)
    var phone: String,
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "category_id", insertable = false, updatable = false)
    val category: ContactCategory?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Contact
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
