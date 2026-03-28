package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.BusArrivalKey
import app.hyuabot.backend.bus.domain.BusDepartureLogKey
import app.hyuabot.backend.bus.domain.BusTimetableKey
import app.hyuabot.backend.bus.service.BusRouteService
import app.hyuabot.backend.codegen.types.BusArrival
import app.hyuabot.backend.codegen.types.BusCompany
import app.hyuabot.backend.codegen.types.BusDepartureLog
import app.hyuabot.backend.codegen.types.BusRealtime
import app.hyuabot.backend.codegen.types.BusRoute
import app.hyuabot.backend.codegen.types.BusRouteStop
import app.hyuabot.backend.codegen.types.BusRouteStopInput
import app.hyuabot.backend.codegen.types.BusRouteType
import app.hyuabot.backend.codegen.types.BusRunningTime
import app.hyuabot.backend.codegen.types.BusRunningTimeEntry
import app.hyuabot.backend.codegen.types.BusStop
import app.hyuabot.backend.codegen.types.BusTimetable
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import graphql.schema.DataFetchingEnvironment
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.CompletableFuture
import app.hyuabot.backend.database.entity.BusDepartureLog as BusDepartureLogEntity
import app.hyuabot.backend.database.entity.BusRealtime as BusRealtimeEntity
import app.hyuabot.backend.database.entity.BusTimetable as BusTimetableEntity

@DgsComponent
class BusDataFetcher(
    private val routeService: BusRouteService,
) {
    @DgsQuery
    fun bus(
        @InputArgument input: List<BusRouteStopInput>,
        dfe: DataFetchingEnvironment,
    ): List<BusRouteStop> {
        if (input.isEmpty()) return emptyList()
        val datesMap =
            input.associate { key ->
                (key.route to key.order) to key.dates
            }
        val weekdaysMap =
            input.associate {
                (it.route to it.order) to it.weekdays
            }
        val limitMap =
            input.associate { key ->
                (key.route to key.order) to key.limit
            }
        val afterMap =
            input.associate { key ->
                (key.route to key.order) to key.after
            }
        dfe.graphQlContext.put("datesMap", datesMap)
        dfe.graphQlContext.put("weekdaysMap", weekdaysMap)
        dfe.graphQlContext.put("limitMap", limitMap)
        dfe.graphQlContext.put("afterMap", afterMap)
        return routeService.fetchRouteStops(input).map {
            BusRouteStop(
                route =
                    BusRoute(
                        seq = it.routeID,
                        name = it.route!!.name,
                        type =
                            BusRouteType(
                                code = it.route!!.typeCode,
                                name = it.route!!.typeName,
                            ),
                        company =
                            BusCompany(
                                seq = it.route!!.companyID,
                                name = it.route!!.companyName,
                                telephone = it.route!!.companyPhone,
                            ),
                        runningTime =
                            BusRunningTime(
                                up =
                                    BusRunningTimeEntry(
                                        first = it.route!!.upFirstTime,
                                        last = it.route!!.upLastTime,
                                    ),
                                down =
                                    BusRunningTimeEntry(
                                        first = it.route!!.downFirstTime,
                                        last = it.route!!.downLastTime,
                                    ),
                            ),
                    ),
                stop =
                    BusStop(
                        seq = it.stopID,
                        name = it.stop!!.name,
                        districtCode = it.stop!!.districtCode,
                        region = it.stop!!.regionName,
                        mobileNumber = it.stop!!.mobileNumber,
                        latitude = it.stop!!.latitude,
                        longitude = it.stop!!.longitude,
                    ),
                startStop =
                    BusStop(
                        seq = it.startStopID,
                        name = it.startStop!!.name,
                        districtCode = it.startStop!!.districtCode,
                        region = it.startStop!!.regionName,
                        mobileNumber = it.startStop!!.mobileNumber,
                        latitude = it.startStop!!.latitude,
                        longitude = it.startStop!!.longitude,
                    ),
                order = it.order,
                minutes = it.minuteFromStart,
                realtime = emptyList(),
                timetable = emptyList(),
                log = emptyList(),
                arrival = emptyList(),
            )
        }
    }

    @DgsData(parentType = "BusRouteStop")
    fun realtime(dfe: DataFetchingEnvironment): CompletableFuture<List<BusRealtime>> {
        val routeStop = dfe.getSource<BusRouteStop>()!!
        val key = routeStop.route.seq to routeStop.stop.seq
        val dataLoader = dfe.getDataLoader<Pair<Int, Int>, List<BusRealtimeEntity>>("busRealtimeDataLoader")!!
        return dataLoader.load(key).thenApply { realtimeList ->
            realtimeList.map {
                BusRealtime(
                    order = it.order,
                    stops = it.remainingStop,
                    seats = it.remainingSeat,
                    minutes = it.remainingTime.toMinutes().toInt(),
                    lowFloor = it.isLowFloor,
                    updatedAt = it.updatedAt.withZoneSameInstant(LocalDateTimeBuilder.serviceTimezone),
                )
            }
        }
    }

    @DgsData(parentType = "BusRouteStop")
    fun timetable(dfe: DataFetchingEnvironment): CompletableFuture<List<BusTimetable>> {
        val routeStop = dfe.getSource<BusRouteStop>()!!
        val weekdaysMap = dfe.graphQlContext.get<Map<Pair<Int, Int>, List<String>?>>("weekdaysMap")
        val afterMap = dfe.graphQlContext.get<Map<Pair<Int, Int>, LocalTime>>("afterMap")
        val routeID = routeStop.route.seq
        val startStopID = routeStop.startStop.seq
        val weekdays = weekdaysMap[routeID to startStopID]
        val after = afterMap[routeID to routeStop.order]
        val key = BusTimetableKey(routeID = routeID, startStopID = startStopID, weekdays = weekdays, after = after)
        val dataLoader = dfe.getDataLoader<BusTimetableKey, List<BusTimetableEntity>>("busTimetableDataLoader")!!
        return dataLoader.load(key).thenApply { timetableList ->
            timetableList.map {
                BusTimetable(
                    seq = it.seq!!,
                    weekday = it.weekday,
                    time = it.departureTime,
                )
            }
        }
    }

    @DgsData(parentType = "BusRouteStop")
    fun log(dfe: DataFetchingEnvironment): CompletableFuture<List<BusDepartureLog>> {
        val routeStop = dfe.getSource<BusRouteStop>()!!
        val datesMap = dfe.graphQlContext.get<Map<Pair<Int, Int>, List<LocalDate>>>("datesMap")
        val routeID = routeStop.route.seq
        val stopID = routeStop.stop.seq
        val dates =
            datesMap[routeID to routeStop.order]
                ?: return CompletableFuture.completedFuture(emptyList())
        val key = BusDepartureLogKey(routeID = routeID, stopID = stopID, dates = dates)
        val dataLoader = dfe.getDataLoader<BusDepartureLogKey, List<BusDepartureLogEntity>>("busDepartureLogDataLoader")!!
        return dataLoader.load(key).thenApply { logList ->
            logList.map {
                BusDepartureLog(
                    seq = it.seq!!,
                    date = it.departureDate,
                    time = it.departureTime,
                    vehicle = it.vehicleID,
                )
            }
        }
    }

    @DgsData(parentType = "BusRouteStop")
    fun arrival(dfe: DataFetchingEnvironment): CompletableFuture<List<BusArrival>> {
        val routeStop = dfe.getSource<BusRouteStop>()!!
        val limitMap = dfe.graphQlContext.get<Map<Pair<Int, Int>, Int>>("limitMap")
        val key =
            BusArrivalKey(
                routeID = routeStop.route.seq,
                stopID = routeStop.stop.seq,
                startStopID = routeStop.startStop.seq,
                limit = limitMap[routeStop.route.seq to routeStop.order],
            )
        val dataLoader = dfe.getDataLoader<BusArrivalKey, List<BusArrival>>("busArrivalDataLoader")!!
        return dataLoader.load(key)
    }
}
