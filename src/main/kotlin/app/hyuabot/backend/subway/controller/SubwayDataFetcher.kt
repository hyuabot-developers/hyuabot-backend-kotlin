package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.codegen.types.SubwayArrival
import app.hyuabot.backend.codegen.types.SubwayArrivalGroup
import app.hyuabot.backend.codegen.types.SubwayInput
import app.hyuabot.backend.codegen.types.SubwayOriginTerminal
import app.hyuabot.backend.codegen.types.SubwayRealtime
import app.hyuabot.backend.codegen.types.SubwayStation
import app.hyuabot.backend.codegen.types.SubwayTimetable
import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.subway.domain.SubwayTimetableKey
import app.hyuabot.backend.subway.service.SubwayService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@DgsComponent
class SubwayDataFetcher(
    private val subwayService: SubwayService,
    private val publicHolidayService: PublicHolidayService,
) {
    @DgsQuery
    fun subway(
        @InputArgument input: SubwayInput,
        dfe: DgsDataFetchingEnvironment,
    ): List<SubwayStation> {
        if (input.keys.isEmpty()) return emptyList()
        val filterMap =
            input.keys
                .groupBy {
                    it.stationID
                }.mapValues { (_, keys) ->
                    val directions = keys.flatMap { it.direction }.map(::normalizeSubwayDirection).distinct()
                    val weekdays = keys.flatMap { it.weekdays }.map(::normalizeSubwayWeekday).distinct()
                    directions to weekdays
                }
        dfe.graphQlContext.put("filterMap", filterMap)
        val limitMap =
            input.keys.associate {
                it.stationID to it.limit
            }
        dfe.graphQlContext.put("limitMap", limitMap)
        // distinct + sorted so the cache key is insensitive to request order/duplicates
        return subwayService.getStationViews(
            input.keys
                .map { it.stationID }
                .distinct()
                .sorted(),
        )
    }

    @DgsData(parentType = "SubwayStation")
    fun realtime(dfe: DgsDataFetchingEnvironment): List<SubwayRealtime> {
        val station = dfe.getSource<SubwayStation>()!!
        val filterMap = dfe.graphQlContext.get<Map<String, Pair<List<String>, List<String>>>>("filterMap")
        val (directions, _) = filterMap[station.stationID]!!
        val entries =
            subwayService.getRealtimeList(
                station.stationID,
                directions = directions.flatMap(::subwayDirectionAliases).distinct(),
            )

        return entries.map {
            SubwayRealtime(
                order = it.order,
                location = it.location,
                stops = it.remainingStop,
                minutes = it.remainingTime.toMinutes().toInt(),
                direction = normalizeSubwayDirection(it.heading),
                terminal = it.terminalStation!!.toSubwayStation(),
                trainNumber = it.trainNumber,
                isExpress = it.isExpress,
                isLast = it.isLast,
                status = it.status,
                updatedAt = it.updatedAt.withZoneSameInstant(LocalDateTimeBuilder.serviceTimezone),
            )
        }
    }

    @DgsData(parentType = "SubwayStation")
    fun timetable(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<SubwayTimetable>> {
        val station = dfe.getSource<SubwayStation>()!!
        val filterMap = dfe.graphQlContext.get<Map<String, Pair<List<String>, List<String>>>>("filterMap")
        val (directions, weekdays) = filterMap[station.stationID]!!
        val key =
            SubwayTimetableKey(
                stationID = station.stationID,
                directions = directions,
                weekdays = weekdays,
            )
        val dataLoader =
            dfe.getDataLoader<SubwayTimetableKey, List<SubwayTimetable>>(
                "subwayTimetableDataLoader",
            )!!
        return dataLoader.load(key)
    }

    @DgsData(parentType = "SubwayStation")
    fun arrival(dfe: DgsDataFetchingEnvironment): List<SubwayArrivalGroup> {
        val station = dfe.getSource<SubwayStation>()!!
        val filterMap = dfe.graphQlContext.get<Map<String, Pair<List<String>, List<String>>>>("filterMap")
        val (directions, weekdays) = filterMap[station.stationID]!!
        if (weekdays.isEmpty()) {
            return emptyList()
        } else if (weekdays.size > 1) {
            throw IllegalArgumentException(
                "arrival query expects exactly one weekday, but got: $weekdays for station ${station.stationID}",
            )
        }
        val today = LocalDate.now(LocalDateTimeBuilder.serviceTimezone)
        val weekday = if (publicHolidayService.findPublicHoliday(today) != null) "weekends" else weekdays.first()
        val limitMap = dfe.graphQlContext.get<Map<String, Int>>("limitMap")
        val limit = limitMap[station.stationID]
        return directions.map { direction ->
            val entries =
                subwayService
                    .getArrival(
                        stationID = station.stationID,
                        directions = subwayDirectionAliases(direction),
                        weekday = weekday,
                        limit = null,
                    ).flatMap { it.entries }
                    .distinctBy {
                        listOf(
                            it.isRealtime,
                            it.trainNumber,
                            it.minutes,
                            it.location,
                            it.terminal.stationID,
                        )
                    }.filterTimetableAfterRealtime()
                    .sortedWith(compareBy<SubwayArrival> { !it.isRealtime }.thenBy { it.minutes })
                    .let { if (limit != null) it.take(limit) else it }
            SubwayArrivalGroup(
                direction = direction,
                entries = entries,
            )
        }
    }

    private fun SubwayRouteStation.toSubwayStation() =
        SubwayOriginTerminal(
            stationID = id,
            name = name,
        )

    private fun normalizeSubwayWeekday(weekday: String): String =
        when (weekday) {
            "saturday", "sunday" -> "weekends"
            else -> weekday
        }

    private fun normalizeSubwayDirection(direction: String): String =
        when (direction) {
            "0" -> "up"
            "1" -> "down"
            else -> direction
        }

    private fun subwayDirectionAliases(direction: String): List<String> =
        when (normalizeSubwayDirection(direction)) {
            "up" -> listOf("up", "0")
            "down" -> listOf("down", "1")
            else -> listOf(direction)
        }

    private fun List<SubwayArrival>.filterTimetableAfterRealtime(): List<SubwayArrival> {
        val lastRealtimeMinutes = filter { it.isRealtime }.maxOfOrNull { it.minutes } ?: return this
        return filter { it.isRealtime || it.minutes >= lastRealtimeMinutes + MIN_TIMETABLE_GAP_MINUTES }
    }

    companion object {
        private const val MIN_TIMETABLE_GAP_MINUTES = 5
    }
}
