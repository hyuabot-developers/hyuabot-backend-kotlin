package app.hyuabot.backend.shuttle.controller

import app.hyuabot.backend.codegen.types.Shuttle
import app.hyuabot.backend.codegen.types.ShuttleArrival
import app.hyuabot.backend.codegen.types.ShuttleHoliday
import app.hyuabot.backend.codegen.types.ShuttleInput
import app.hyuabot.backend.codegen.types.ShuttleLimitInput
import app.hyuabot.backend.codegen.types.ShuttlePeriod
import app.hyuabot.backend.codegen.types.ShuttleRoute
import app.hyuabot.backend.codegen.types.ShuttleServiceNotice
import app.hyuabot.backend.codegen.types.ShuttleStop
import app.hyuabot.backend.codegen.types.ShuttleTimetable
import app.hyuabot.backend.codegen.types.ShuttleTimetableEntry
import app.hyuabot.backend.codegen.types.ShuttleTimetableGroup
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.shuttle.domain.ShuttleFilterContext
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableKey
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableResult
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableViewItem
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import app.hyuabot.backend.shuttle.service.ShuttleStopService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@DgsComponent
class ShuttleDataFetcher(
    private val holidayService: ShuttleHolidayService,
    private val publicHolidayService: PublicHolidayService,
    private val periodService: ShuttlePeriodService,
    private val stopService: ShuttleStopService,
) {
    @DgsQuery
    fun shuttle(
        @InputArgument input: ShuttleInput,
        dfe: DgsDataFetchingEnvironment,
    ): Shuttle {
        if (input.date != null && (!input.periods.isNullOrEmpty() || !input.weekdays.isNullOrEmpty())) {
            throw IllegalArgumentException("date and periods/weekdays cannot be used together")
        }
        val (filterPeriods, filterWeekdays, isHalt) = resolveFilter(input)
        dfe.graphQlContext.put(
            "filter",
            ShuttleFilterContext(
                stops = input.stops,
                periods = filterPeriods,
                weekdays = filterWeekdays,
                date = input.date,
                after = input.after,
                isHalt = isHalt,
            ),
        )
        return Shuttle(
            period = null,
            holiday = null,
            serviceNotices = emptyList(),
            stops = emptyList(),
        )
    }

    @DgsData(parentType = "Shuttle")
    fun period(dfe: DgsDataFetchingEnvironment): ShuttlePeriod? {
        val date =
            dfe.graphQlContext.get<ShuttleFilterContext>("filter").date ?: return null
        return periodService.findShuttlePeriod(date)?.let {
            ShuttlePeriod(
                seq = it.seq!!,
                start = it.start.withZoneSameInstant(LocalDateTimeBuilder.serviceTimezone),
                end = it.end.withZoneSameInstant(LocalDateTimeBuilder.serviceTimezone),
                type = it.type,
            )
        }
    }

    @DgsData(parentType = "Shuttle")
    fun holiday(dfe: DgsDataFetchingEnvironment): ShuttleHoliday? {
        val date = dfe.graphQlContext.get<ShuttleFilterContext>("filter").date ?: return null
        return holidayService.findShuttleHoliday(date)?.let {
            ShuttleHoliday(
                seq = it.seq!!,
                date = it.date,
                type = it.type,
                calendar = it.calendarType,
            )
        }
    }

    @DgsData(parentType = "Shuttle")
    fun serviceNotices(
        @InputArgument start: LocalDate,
        @InputArgument end: LocalDate,
    ): List<ShuttleServiceNotice> {
        require(!start.isAfter(end)) { "Start date must be before or equal to end date" }
        val periodNotices =
            periodService.findShuttlePeriodsStartingBetween(start, end).map {
                val serviceDate = it.start.withZoneSameInstant(LocalDateTimeBuilder.serviceTimezone).toLocalDate()
                ShuttleServiceNotice(
                    id = "period:${it.seq}:$serviceDate",
                    kind = "period",
                    date = serviceDate,
                    period =
                        ShuttlePeriod(
                            seq = it.seq!!,
                            start = it.start.withZoneSameInstant(LocalDateTimeBuilder.serviceTimezone),
                            end = it.end.withZoneSameInstant(LocalDateTimeBuilder.serviceTimezone),
                            type = it.type,
                        ),
                    holiday = null,
                )
            }
        val holidayNotices =
            holidayService.findShuttleHolidayOccurrences(start, end).map {
                ShuttleServiceNotice(
                    id = "holiday:${it.holiday.seq}:${it.date}",
                    kind = "holiday",
                    date = it.date,
                    period = null,
                    holiday =
                        ShuttleHoliday(
                            seq = it.holiday.seq!!,
                            date = it.holiday.date,
                            type = it.holiday.type,
                            calendar = it.holiday.calendarType,
                        ),
                )
            }
        return (periodNotices + holidayNotices).sortedWith(compareBy({ it.date }, { it.id }))
    }

    @DgsData(parentType = "Shuttle")
    fun stops(dfe: DgsDataFetchingEnvironment): List<ShuttleStop> {
        val filter = dfe.graphQlContext.get<ShuttleFilterContext>("filter")
        return filter.stops?.let {
            stopService.getStopsByNames(filter.stops.map { it.name }).map {
                ShuttleStop(
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timetable =
                        ShuttleTimetable(
                            destination = emptyList(),
                            order = emptyList(),
                        ),
                )
            }
        } ?: stopService.getAllStops().map {
            ShuttleStop(
                name = it.name,
                latitude = it.latitude,
                longitude = it.longitude,
                timetable =
                    ShuttleTimetable(
                        destination = emptyList(),
                        order = emptyList(),
                    ),
            )
        }
    }

    @DgsData(parentType = "ShuttleStop")
    fun timetable(dfe: DgsDataFetchingEnvironment): CompletableFuture<ShuttleTimetable> {
        val stop = dfe.getSource<ShuttleStop>()!!
        val filter = dfe.graphQlContext.get<ShuttleFilterContext>("filter")
        val stopFilter = filter.stops?.first { it.name == stop.name }
        val limit =
            if (filter.stops == null) {
                ShuttleLimitInput(order = null, destination = null)
            } else {
                filter.stops.first { it.name == stop.name }.limit
            }
        val key =
            ShuttleTimetableKey(
                stop = stop.name,
                periods = filter.periods.toSet(),
                weekdays = filter.weekdays.toSet(),
                routes = stopFilter?.routes?.toSet(),
                tags = stopFilter?.tags?.toSet(),
                destinations = stopFilter?.destinations?.toSet(),
                after = filter.after,
                limit = limit,
            )
        if (filter.isHalt) {
            return CompletableFuture.completedFuture(
                ShuttleTimetable(
                    order = emptyList(),
                    destination = emptyList(),
                ),
            )
        }
        val dataLoader = dfe.getDataLoader<ShuttleTimetableKey, ShuttleTimetableResult>("shuttleTimetableDataLoader")!!
        return dataLoader.load(key).thenApply { result ->
            ShuttleTimetable(
                order = result.order.map { it.toTimetableEntry() },
                destination =
                    result.destination.map { (dest, entries) ->
                        ShuttleTimetableGroup(
                            destination = dest,
                            entries = entries.map { it.toTimetableEntry() },
                        )
                    },
            )
        }
    }

    private fun resolveFilter(input: ShuttleInput): Triple<List<String>, List<Boolean>, Boolean> {
        if (!input.periods.isNullOrEmpty() && !input.weekdays.isNullOrEmpty()) {
            return Triple(input.periods, input.weekdays, false)
        }
        val date = input.date ?: LocalDate.now(LocalDateTimeBuilder.serviceTimezone)
        val isHoliday = holidayService.findShuttleHoliday(date)
        val periods = periodService.findShuttlePeriod(date) ?: return Triple(emptyList(), emptyList(), false)
        if (isHoliday != null) {
            return if (isHoliday.type == "weekends") {
                Triple(listOf(periods.type), listOf(false), false)
            } else {
                Triple(listOf(periods.type), listOf(false), true)
            }
        }
        if (publicHolidayService.findPublicHoliday(date) != null) {
            return Triple(listOf(periods.type), listOf(false), false)
        }
        val isWeekends = date.dayOfWeek.value == 6 || date.dayOfWeek.value == 7
        return Triple(listOf(periods.type), listOf(!isWeekends), false)
    }

    private fun ShuttleTimetableViewItem.toTimetableEntry() =
        ShuttleTimetableEntry(
            seq = seq,
            route =
                ShuttleRoute(
                    name = routeName,
                    tag = routeTag,
                ),
            period = period,
            weekday = weekday,
            time = time,
            stops =
                stops.map {
                    ShuttleArrival(
                        stop = it.stop,
                        time = it.time,
                    )
                },
        )
}
