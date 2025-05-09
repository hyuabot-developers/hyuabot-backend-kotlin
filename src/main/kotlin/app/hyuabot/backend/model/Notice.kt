package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "notice")
@Table(name = "notices")
data class Notice(
    @Id
    @Column(name = "notice_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notice_notice_id_seq")
    val id: Int,
    @Column(name = "title", length = 100)
    val title: String,
    @Column(name = "url", length = 200)
    val url: String,
    @Column(name = "expired_at", columnDefinition = "timestamptz")
    val expiredAt: ZonedDateTime,
    @Column(name = "category_id")
    val categoryID: Int,
    @Column(name = "user_id", length = 20)
    val userID: String,
    @Column(name = "language", length = 10)
    val language: String,
)
