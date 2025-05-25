package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "notice_category")
@Table(name = "notice_category")
@SequenceGenerator(name = "notice_category_category_id_seq", allocationSize = 1)
data class NoticeCategory(
    @Id
    @Column(name = "category_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_category_category_id_seq")
    val id: Int,
    @Column(name = "category_name", length = 20, nullable = false)
    val name: String,
    @OneToMany(mappedBy = "category")
    val notice: List<Notice> = emptyList(),
)
