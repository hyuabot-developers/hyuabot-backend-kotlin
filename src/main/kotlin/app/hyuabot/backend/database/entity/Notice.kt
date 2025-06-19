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
import java.time.ZonedDateTime

@Entity(name = "notice")
@Table(name = "notices")
@SequenceGenerator(name = "notices_notice_id_seq", allocationSize = 1)
data class Notice(
    @Id
    @Column(name = "notice_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notices_notice_id_seq")
    val id: Int? = null,
    @Column(name = "title", length = 100, nullable = false)
    val title: String,
    @Column(name = "url", length = 200, nullable = false)
    val url: String,
    @Column(name = "expired_at", columnDefinition = "timestamptz", nullable = false)
    val expiredAt: ZonedDateTime,
    @Column(name = "category_id", columnDefinition = "integer", nullable = false)
    val categoryID: Int,
    @Column(name = "user_id", length = 20, nullable = false)
    val userID: String,
    @Column(name = "language", length = 10, nullable = false)
    val language: String,
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "category_id", insertable = false, updatable = false)
    val category: NoticeCategory,
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    val user: User,
)
