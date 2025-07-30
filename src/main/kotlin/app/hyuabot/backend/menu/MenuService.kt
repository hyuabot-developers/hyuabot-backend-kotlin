package app.hyuabot.backend.menu

import app.hyuabot.backend.cafeteria.CafeteriaService
import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.database.entity.Menu
import app.hyuabot.backend.database.repository.CafeteriaRepository
import app.hyuabot.backend.database.repository.MenuRepository
import app.hyuabot.backend.menu.domain.MenuRequest
import app.hyuabot.backend.menu.exception.MenuNotFoundException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class MenuService(
    private val cafeteriaRepository: CafeteriaRepository,
    private val menuRepository: MenuRepository,
    private val cafeteriaService: CafeteriaService,
) {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getMenuList(
        cafeteriaID: Int?,
        date: LocalDate?,
        type: String?,
    ): List<Menu> =
        when {
            cafeteriaID != null && date != null && type != null -> {
                menuRepository.findByRestaurantIDAndDateAndType(cafeteriaID, date, type)
            }
            cafeteriaID != null && date != null -> {
                menuRepository.findByRestaurantIDAndDate(cafeteriaID, date)
            }
            cafeteriaID != null && type != null -> {
                menuRepository.findByRestaurantIDAndType(cafeteriaID, type)
            }
            date != null && type != null -> {
                menuRepository.findByDateAndType(date, type)
            }
            cafeteriaID != null -> {
                menuRepository.findByRestaurantID(cafeteriaID)
            }
            date != null -> {
                menuRepository.findByDate(date)
            }
            type != null -> {
                menuRepository.findByType(type)
            }
            else -> {
                menuRepository.findAll()
            }
        }

    fun getMenuById(seq: Int): Menu =
        menuRepository.findById(seq).orElseThrow {
            MenuNotFoundException()
        }

    fun createMenu(payload: MenuRequest): Menu {
        if (!cafeteriaRepository.existsById(payload.cafeteriaID)) {
            throw CafeteriaNotFoundException()
        }
        return menuRepository.save(
            Menu(
                restaurantID = payload.cafeteriaID,
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
        payload: MenuRequest,
    ): Menu {
        val menu =
            menuRepository.findById(seq).orElseThrow { MenuNotFoundException() }.let {
                if (!cafeteriaRepository.existsById(payload.cafeteriaID)) {
                    throw CafeteriaNotFoundException()
                }
                it.apply {
                    restaurantID = payload.cafeteriaID
                    date = LocalDate.parse(payload.date, dateFormatter)
                    type = payload.type
                    food = payload.food
                    price = payload.price
                }
            }
        return menuRepository.save(menu)
    }

    fun deleteMenuById(seq: Int) {
        if (!menuRepository.existsById(seq)) {
            throw MenuNotFoundException()
        }
        menuRepository.deleteById(seq)
    }
}
