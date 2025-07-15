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
import java.time.LocalDate

@Entity(name = "menu")
@Table(
    name = "menu",
    indexes = [
        Index(name = "idx_menu", columnList = "restaurant_id, feed_date, time_type, menu_food, menu_price", unique = true),
    ],
)
@SequenceGenerator(name = "menu_seq_seq", allocationSize = 1)
data class Menu(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_seq_seq")
    val seq: Int,
    @Column(name = "restaurant_id", columnDefinition = "integer", nullable = false)
    val restaurantID: Int,
    @Column(name = "feed_date", columnDefinition = "date", nullable = false)
    val date: LocalDate,
    @Column(name = "time_type", length = 10, nullable = false)
    val type: String,
    @Column(name = "menu_food", length = 400, nullable = false)
    val food: String,
    @Column(name = "menu_price", length = 30, nullable = false)
    val price: String,
    @JoinColumn(name = "restaurant_id", referencedColumnName = "restaurant_id", insertable = false, updatable = false)
    @ManyToOne
    val cafeteria: Cafeteria,
)
