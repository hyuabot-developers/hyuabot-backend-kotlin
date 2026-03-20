package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.codegen.types.SubwayInput
import app.hyuabot.backend.codegen.types.SubwayRealtime
import app.hyuabot.backend.codegen.types.SubwayRoute
import app.hyuabot.backend.codegen.types.SubwayStation
import app.hyuabot.backend.codegen.types.SubwayTimetable
import app.hyuabot.backend.subway.domain.SubwayTimetableKey
import app.hyuabot.backend.subway.service.SubwayService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import java.time.ZoneId
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
            input.keys.associate {
                it.stationID to (it.direction to it.weekdays)
            }
        dfe.graphQlContext.put("filterMap", filterMap)
        return subwayService.getStations(input.keys.map { it.stationID }).map { station ->
            SubwayStation(
                stationID = station.id,
                name = station.name,
                order = station.order,
                minutes = station.cumulativeTime.toMinutes().toInt(),
                route =
                    SubwayRoute(
                        seq = station.route!!.id,
                        name = station.route!!.name,
                    ),
                realtime = emptyList(),
                timetable = emptyList(),
            )
        }
    }

    @DgsData(parentType = "SubwayStation")
    fun realtime(dfe: DgsDataFetchingEnvironment): List<SubwayRealtime> {
        val station = dfe.getSource<SubwayStation>() ?: return emptyList()
        val filterMap = dfe.graphQlContext.get<Map<String, Pair<List<String>, List<String>>>>("filterMap")
        val (direction, _) = filterMap[station.stationID] ?: return emptyList()
        val zoneId = ZoneId.of("Asia/Seoul")
        return subwayService.getRealtimeList(station.stationID, directions = direction).map {
            SubwayRealtime(
                order = it.order,
                location = it.location,
                stops = it.remainingStop,
                minutes = it.remainingTime.toMinutes().toInt(),
                direction = it.heading,
                terminal =
                    SubwayStation(
                        stationID = it.terminalStation!!.id,
                        name = it.terminalStation!!.name,
                        order = it.terminalStation!!.order,
                        minutes =
                            it.terminalStation!!
                                .cumulativeTime
                                .toMinutes()
                                .toInt(),
                        route =
                            SubwayRoute(
                                seq = it.terminalStation!!.route!!.id,
                                name = it.terminalStation!!.name,
                            ),
                        realtime = emptyList(),
                        timetable = emptyList(),
                    ),
                trainNumber = it.trainNumber,
                isExpress = it.isExpress,
                isLast = it.isLast,
                status = it.status,
                updatedAt = it.updatedAt.withZoneSameInstant(zoneId),
            )
        }
    }

    @DgsData(parentType = "SubwayStation")
    fun timetable(dfe: DgsDataFetchingEnvironment): CompletableFuture<List<SubwayTimetable>> {
        val station = dfe.getSource<SubwayStation>() ?: return CompletableFuture.completedFuture(emptyList())
        val filterMap = dfe.graphQlContext.get<Map<String, Pair<List<String>, List<String>>>>("filterMap")
        val (direction, weekdays) = filterMap[station.stationID] ?: return CompletableFuture.completedFuture(emptyList())
        val key =
            SubwayTimetableKey(
                stationID = station.stationID,
                directions = direction,
                weekdays = weekdays,
            )
        val dataLoader =
            dfe.getDataLoader<SubwayTimetableKey, List<app.hyuabot.backend.database.entity.SubwayTimetable>>(
                "SubwayTimetableDataLoader",
            ) ?: return CompletableFuture.completedFuture(emptyList())
        return dataLoader.load(key).thenApply { timetables ->
            timetables.map {
                SubwayTimetable(
                    seq = it.seq!!,
                    time = it.departureTime,
                    weekday = it.weekday,
                    direction = it.heading,
                    origin =
                        SubwayStation(
                            stationID = it.startStation!!.id,
                            name = it.startStation!!.name,
                            order = it.startStation!!.order,
                            minutes =
                                it.startStation!!
                                    .cumulativeTime
                                    .toMinutes()
                                    .toInt(),
                            route =
                                SubwayRoute(
                                    seq = it.startStation!!.route!!.id,
                                    name = it.startStation!!.name,
                                ),
                            realtime = emptyList(),
                            timetable = emptyList(),
                        ),
                    terminal =
                        SubwayStation(
                            stationID = it.terminalStation!!.id,
                            name = it.terminalStation!!.name,
                            order = it.terminalStation!!.order,
                            minutes =
                                it.terminalStation!!
                                    .cumulativeTime
                                    .toMinutes()
                                    .toInt(),
                            route =
                                SubwayRoute(
                                    seq = it.terminalStation!!.route!!.id,
                                    name = it.terminalStation!!.name,
                                ),
                            realtime = emptyList(),
                            timetable = emptyList(),
                        ),
                )
            }
        }
    }
}
