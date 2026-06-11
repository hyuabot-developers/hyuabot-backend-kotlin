package app.hyuabot.backend.calendar.controller

import app.hyuabot.backend.calendar.CalendarService
import app.hyuabot.backend.codegen.types.AcademicCalendar
import app.hyuabot.backend.codegen.types.AcademicCalendarCategory
import app.hyuabot.backend.codegen.types.AcademicCalendarEvent
import app.hyuabot.backend.codegen.types.AcademicCalendarInput
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import graphql.schema.DataFetchingEnvironment
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@DgsComponent
class CalendarDataFetcher(
    private val calendarService: CalendarService,
) {
    @DgsQuery
    fun calendar(
        @InputArgument input: AcademicCalendarInput?,
        env: DgsDataFetchingEnvironment,
    ) {
        input?.let {
            env.graphQlContext.put("input", input)
        }
        AcademicCalendar(version = "", categories = emptyList())
    }

    @DgsData(parentType = "AcademicCalendar")
    fun version(): String = calendarService.getCalendarVersion().name

    @DgsData(parentType = "AcademicCalendar")
    @Transactional(readOnly = true)
    fun categories(env: DataFetchingEnvironment): List<AcademicCalendarCategory> {
        val input = env.graphQlContext.get<AcademicCalendarInput>("input")
        return calendarService
            .fetchCalendarEvents(
                category = input?.category,
                start = input?.start ?: LocalDate.parse("2020-01-01"),
                end = input?.end ?: LocalDate.parse("2100-01-01"),
            ).map { category ->
                AcademicCalendarCategory(
                    seq = category.id!!,
                    name = category.name,
                    events =
                        category.event.map { event ->
                            AcademicCalendarEvent(
                                seq = event.id!!,
                                title = event.title,
                                description = event.description,
                                start = event.start,
                                end = event.end,
                            )
                        },
                )
            }
    }
}
