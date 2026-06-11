package app.hyuabot.backend.calendar

import app.hyuabot.backend.calendar.domain.CalendarCategoryRequest
import app.hyuabot.backend.calendar.domain.CalendarEventRequest
import app.hyuabot.backend.calendar.exception.CalendarCategoryNotFoundException
import app.hyuabot.backend.calendar.exception.CalendarEventNotFoundException
import app.hyuabot.backend.calendar.exception.CalendarVersionNotFoundException
import app.hyuabot.backend.calendar.exception.DuplicateCategoryException
import app.hyuabot.backend.database.entity.CalendarCategory
import app.hyuabot.backend.database.entity.CalendarEvent
import app.hyuabot.backend.database.entity.CalendarVersion
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.database.repository.CalendarCategoryRepository
import app.hyuabot.backend.database.repository.CalendarEventRepository
import app.hyuabot.backend.database.repository.CalendarVersionRepository
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class CalendarService(
    private val categoryRepository: CalendarCategoryRepository,
    private val eventRepository: CalendarEventRepository,
    private val versionRepository: CalendarVersionRepository,
) {
    fun getCalendarVersion(): CalendarVersion =
        versionRepository.findTopByOrderByCreatedAtDesc()
            ?: throw CalendarVersionNotFoundException()

    fun getCalendarCategories(): List<CalendarCategory> = categoryRepository.findAll().sortedBy { it.id }

    fun createCalendarCategory(payload: CalendarCategoryRequest): CalendarCategory {
        categoryRepository.findByName(payload.name)?.let {
            throw DuplicateCategoryException()
        }
        updateCalendarVersion()
        return categoryRepository.save(
            CalendarCategory(
                name = payload.name,
                event = mutableListOf(),
            ),
        )
    }

    fun getCalendarCategoryById(id: Int): CalendarCategory =
        categoryRepository.findById(id).orElseThrow { CalendarCategoryNotFoundException() }

    fun updateCalendarCategoryById(
        id: Int,
        payload: CalendarCategoryRequest,
    ): CalendarCategory {
        categoryRepository.findById(id).orElseThrow { CalendarCategoryNotFoundException() }.let { category ->
            categoryRepository.findByName(payload.name)?.let {
                if (it.id!! != id) {
                    throw DuplicateCategoryException()
                }
            }
            category.name = payload.name
            updateCalendarVersion()
            return categoryRepository.save(category)
        }
    }

    @Transactional
    fun deleteCalendarCategoryById(id: Int) {
        categoryRepository.findById(id).orElseThrow { CalendarCategoryNotFoundException() }.let { category ->
            // 해당 카테고리에 속한 일정도 모두 삭제
            eventRepository.deleteAll(category.event)
            categoryRepository.delete(category)
            updateCalendarVersion()
        }
    }

    fun getAllEvents(): List<CalendarEvent> = eventRepository.findAll().sortedBy { it.id }

    @Transactional(readOnly = true)
    fun getEventByCategoryId(id: Int): List<CalendarEvent> {
        categoryRepository.findById(id).orElseThrow { CalendarCategoryNotFoundException() }.let { category ->
            return category.event.sortedBy { it.id }
        }
    }

    fun createEvent(payload: CalendarEventRequest): CalendarEvent {
        if (!LocalDateTimeBuilder.checkLocalDateRange(payload.start, payload.end)) {
            throw LocalDateTimeNotValidException()
        }
        categoryRepository
            .findById(payload.categoryID)
            .orElseThrow { CalendarCategoryNotFoundException() }
        updateCalendarVersion()
        return eventRepository.save(
            CalendarEvent(
                title = payload.title,
                description = payload.description,
                start = LocalDate.parse(payload.start),
                end = LocalDate.parse(payload.end),
                categoryID = payload.categoryID,
                category = null,
            ),
        )
    }

    fun getEventById(id: Int): CalendarEvent = eventRepository.findById(id).orElseThrow { CalendarEventNotFoundException() }

    fun updateEvent(
        id: Int,
        payload: CalendarEventRequest,
    ): CalendarEvent {
        if (!LocalDateTimeBuilder.checkLocalDateRange(payload.start, payload.end)) {
            throw LocalDateTimeNotValidException()
        }
        categoryRepository
            .findById(payload.categoryID)
            .orElseThrow { CalendarCategoryNotFoundException() }
        updateCalendarVersion()
        return eventRepository.findById(id).orElseThrow { CalendarEventNotFoundException() }.let { event ->
            event.apply {
                title = payload.title
                description = payload.description
                start = LocalDate.parse(payload.start)
                end = LocalDate.parse(payload.end)
                categoryID = payload.categoryID
            }
            eventRepository.save(event)
        }
    }

    fun deleteEventById(id: Int) {
        eventRepository.findById(id).orElseThrow { CalendarEventNotFoundException() }.let { event ->
            eventRepository.delete(event)
            updateCalendarVersion()
        }
    }

    fun updateCalendarVersion() {
        val now = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        val versionName = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        versionRepository.findAll().let { versions ->
            if (versions.isEmpty()) {
                versionRepository.save(
                    CalendarVersion(
                        name = versionName,
                        createdAt = now,
                    ),
                )
            } else {
                versions.first().let { current ->
                    current.name = versionName
                    current.createdAt = now
                    versionRepository.save(current)
                }
            }
        }
    }

    fun fetchCalendarEvents(
        category: String?,
        start: LocalDate = LocalDate.parse("1900-01-01"),
        end: LocalDate = LocalDate.parse("2100-12-31"),
    ): List<CalendarCategory> {
        val categories = eventRepository.findCategoriesWithEventsBetween(start, end)
        return categories
            .filter { cat ->
                category == null || cat.name.contains(category, ignoreCase = true)
            }.map {
                CalendarCategory(
                    id = it.id,
                    name = it.name,
                    event = it.event.sortedBy { event -> event.id }.toMutableList(),
                )
            }
    }
}
