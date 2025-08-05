package app.hyuabot.backend.cafeteria

import app.hyuabot.backend.cafeteria.domain.MenuRequest
import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.cafeteria.exception.MenuNotFoundException
import app.hyuabot.backend.database.entity.Menu
import app.hyuabot.backend.database.repository.CafeteriaRepository
import app.hyuabot.backend.database.repository.MenuRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class MenuService(
    private val cafeteriaRepository: CafeteriaRepository,
    private val menuRepository: MenuRepository,
) {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getMenuList(
        cafeteriaID: Int,
        date: LocalDate?,
        type: String?,
    ): List<Menu> =
        when {
            date != null && type != null -> {
                menuRepository.findByRestaurantIDAndDateAndType(cafeteriaID, date, type)
            }
            date != null -> {
                menuRepository.findByRestaurantIDAndDate(cafeteriaID, date)
            }
            type != null -> {
                menuRepository.findByRestaurantIDAndType(cafeteriaID, type)
            }
            else -> {
                menuRepository.findByRestaurantID(cafeteriaID)
            }
        }

    fun getMenuById(
        seq: Int,
        menuSeq: Int,
    ): Menu =
        menuRepository.findByRestaurantIDAndSeq(seq, menuSeq)
            ?: throw MenuNotFoundException()

    fun createMenu(
        seq: Int,
        payload: MenuRequest,
    ): Menu {
        if (!cafeteriaRepository.existsById(seq)) {
            throw CafeteriaNotFoundException()
        }
        return menuRepository.save(
            Menu(
                restaurantID = seq,
                date = LocalDate.parse(payload.date, dateFormatter),
                type = payload.type,
                food = payload.food,
                price = payload.price,
                cafeteria = null,
            ),
        )
    }

    fun updateMenu(
        seq: Int,
        menuSeq: Int,
        payload: MenuRequest,
    ): Menu {
        val menu =
            menuRepository.findByRestaurantIDAndSeq(seq, menuSeq)?.let {
                if (!cafeteriaRepository.existsById(seq)) {
                    throw CafeteriaNotFoundException()
                }
                it.apply {
                    restaurantID = seq
                    date = LocalDate.parse(payload.date, dateFormatter)
                    type = payload.type
                    food = payload.food
                    price = payload.price
                }
            } ?: throw MenuNotFoundException()
        return menuRepository.save(menu)
    }

    fun deleteMenuById(
        seq: Int,
        menuSeq: Int,
    ) {
        menuRepository.findByRestaurantIDAndSeq(seq, menuSeq)?.let {
            menuRepository.delete(it)
        } ?: throw MenuNotFoundException()
    }
}
