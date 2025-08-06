package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MenuRepository : JpaRepository<Menu, Int> {
    fun findByRestaurantIDAndSeq(
        restaurantID: Int,
        seq: Int,
    ): Menu?

    fun findByRestaurantID(restaurantID: Int): List<Menu>

    fun findByDate(date: LocalDate): List<Menu>

    fun findByType(mealType: String): List<Menu>

    fun findByRestaurantIDAndType(
        restaurantID: Int,
        mealType: String,
    ): List<Menu>

    fun findByRestaurantIDAndDate(
        restaurantID: Int,
        date: LocalDate,
    ): List<Menu>

    fun findByDateAndType(
        date: LocalDate,
        mealType: String,
    ): List<Menu>

    fun findByRestaurantIDAndDateAndType(
        restaurantID: Int,
        date: LocalDate,
        mealType: String,
    ): List<Menu>
}
