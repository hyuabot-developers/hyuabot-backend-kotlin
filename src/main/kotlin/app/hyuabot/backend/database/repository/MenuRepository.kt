package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MenuRepository : JpaRepository<Menu, Int> {
    fun findByRestaurantIDAndDate(
        restaurantID: Int,
        date: LocalDate,
    ): List<Menu>

    fun findByRestaurantIDAndDateAndType(
        restaurantID: Int,
        date: LocalDate,
        mealType: String,
    ): List<Menu>
}
