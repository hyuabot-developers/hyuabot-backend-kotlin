package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "notice_category")
@Table(name = "notice_category")
@SequenceGenerator(name = "notice_category_category_id_seq", allocationSize = 1)
class NoticeCategory(
    @Id
    @Column(name = "category_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_category_category_id_seq")
    val id: Int? = null,
    @Column(name = "category_name", length = 20, nullable = false)
    var name: String,
    @OneToMany(mappedBy = "category", fetch = FetchType.EAGER)
    val notice: MutableList<Notice>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as NoticeCategory
        return id != null && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
