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
class Menu(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_seq_seq")
    val seq: Int? = null,
    @Column(name = "restaurant_id", columnDefinition = "integer", nullable = false)
    var restaurantID: Int,
    @Column(name = "feed_date", columnDefinition = "date", nullable = false)
    var date: LocalDate,
    @Column(name = "time_type", length = 10, nullable = false)
    var type: String,
    @Column(name = "menu_food", length = 400, nullable = false)
    var food: String,
    @Column(name = "menu_price", length = 30, nullable = false)
    var price: String,
    @JoinColumn(name = "restaurant_id", referencedColumnName = "restaurant_id", insertable = false, updatable = false)
    @ManyToOne
    val cafeteria: Cafeteria?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Menu
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
