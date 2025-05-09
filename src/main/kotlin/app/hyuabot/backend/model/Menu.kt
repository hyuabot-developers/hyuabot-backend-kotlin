package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity(name = "menu")
@Table(name = "menu")
data class Menu(
    @Id
    @Column(name = "restaurant_id", columnDefinition = "integer")
    val restaurantID: Int,
    @Id
    @Column(name = "feed_date", columnDefinition = "date")
    val date: LocalDate,
    @Id
    @Column(name = "time_type", length = 10)
    val type: String,
    @Id
    @Column(name = "menu_food", length = 400)
    val food: String,
    @Id
    @Column(name = "menu_price", length = 30)
    val price: String,
)
