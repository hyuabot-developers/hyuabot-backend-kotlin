package app.hyuabot.backend.cafeteria

import app.hyuabot.backend.cafeteria.domain.MenuRequest
import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.cafeteria.exception.MenuNotFoundException
import app.hyuabot.backend.database.entity.Menu
import app.hyuabot.backend.database.repository.CafeteriaRepository
import app.hyuabot.backend.database.repository.MenuRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate
import app.hyuabot.backend.codegen.types.Menu as MenuView

@Service
class MenuService(
    private val cafeteriaRepository: CafeteriaRepository,
    private val menuRepository: MenuRepository,
) {
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

    // Daily-static menus shared across all users -> cache the GraphQL DTO (plain, serializes cleanly).
    @Cacheable(cacheNames = ["cafeteriaMenu"], key = "#cafeteriaID + ':' + #date")
    fun getMenuViewByDate(
        cafeteriaID: Int,
        date: LocalDate,
    ): List<MenuView> =
        menuRepository.findByRestaurantIDAndDate(cafeteriaID, date).map {
            MenuView(
                seq = it.seq!!,
                type = it.type,
                food = it.food,
                price = it.price,
            )
        }

    fun getMenuById(
        seq: Int,
        menuSeq: Int,
    ): Menu =
        menuRepository.findByRestaurantIDAndSeq(seq, menuSeq)
            ?: throw MenuNotFoundException()

    @CacheEvict(cacheNames = ["cafeteriaMenu"], allEntries = true)
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
                date = payload.date,
                type = payload.type,
                food = payload.food,
                price = payload.price,
                cafeteria = null,
            ),
        )
    }

    @CacheEvict(cacheNames = ["cafeteriaMenu"], allEntries = true)
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
                    date = payload.date
                    type = payload.type
                    food = payload.food
                    price = payload.price
                }
            } ?: throw MenuNotFoundException()
        return menuRepository.save(menu)
    }

    @CacheEvict(cacheNames = ["cafeteriaMenu"], allEntries = true)
    fun deleteMenuById(
        seq: Int,
        menuSeq: Int,
    ) {
        menuRepository.findByRestaurantIDAndSeq(seq, menuSeq)?.let {
            menuRepository.delete(it)
        } ?: throw MenuNotFoundException()
    }
}
