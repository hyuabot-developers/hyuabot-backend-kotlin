package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.codegen.types.SubwayArrivalGroup
import app.hyuabot.backend.codegen.types.SubwayInput
import app.hyuabot.backend.codegen.types.SubwayOriginTerminal
import app.hyuabot.backend.codegen.types.SubwayRealtime
import app.hyuabot.backend.codegen.types.SubwayStation
import app.hyuabot.backend.codegen.types.SubwayTimetable
import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.subway.domain.SubwayTimetableKey
import app.hyuabot.backend.subway.service.SubwayService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import java.util.concurrent.CompletableFuture

@DgsComponent
class SubwayDataFetcher(
    private val subwayService: SubwayService,
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
                    val directions = keys.flatMap { it.direction }.distinct()
                    val weekdays = keys.flatMap { it.weekdays }.distinct()
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
        return subwayService.getRealtimeList(station.stationID, directions = directions).map {
            SubwayRealtime(
                order = it.order,
                location = it.location,
                stops = it.remainingStop,
                minutes = it.remainingTime.toMinutes().toInt(),
                direction = it.heading,
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
        val weekday = weekdays.first()
        val limitMap = dfe.graphQlContext.get<Map<String, Int>>("limitMap")
        return subwayService
            .getArrival(
                stationID = station.stationID,
                directions = directions,
                weekday = weekday,
                limit = limitMap[station.stationID],
            )
    }

    private fun SubwayRouteStation.toSubwayStation() =
        SubwayOriginTerminal(
            stationID = id,
            name = name,
        )
}
