package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.MenuID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity(name = "menu")
@Table(name = "menu")
@IdClass(MenuID::class)
data class Menu(
    @Id
    @Column(name = "restaurant_id", columnDefinition = "integer", nullable = false)
    val restaurantID: Int,
    @Id
    @Column(name = "feed_date", columnDefinition = "date", nullable = false)
    val date: LocalDate,
    @Id
    @Column(name = "time_type", length = 10, nullable = false)
    val type: String,
    @Id
    @Column(name = "menu_food", length = 400, nullable = false)
    val food: String,
    @Id
    @Column(name = "menu_price", length = 30, nullable = false)
    val price: String,
    @JoinColumn(name = "restaurant_id", referencedColumnName = "restaurant_id", insertable = false, updatable = false)
    @ManyToOne
    val cafeteria: Cafeteria,
)
