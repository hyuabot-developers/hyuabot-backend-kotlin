package app.hyuabot.backend.subway.service

import app.hyuabot.backend.codegen.types.SubwayArrival
import app.hyuabot.backend.codegen.types.SubwayArrivalGroup
import app.hyuabot.backend.codegen.types.SubwayOriginTerminal
import app.hyuabot.backend.database.entity.SubwayRealtime
import app.hyuabot.backend.database.entity.SubwayRoute
import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.database.entity.SubwayStation
import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.SubwayRealtimeRepository
import app.hyuabot.backend.database.repository.SubwayRouteRepository
import app.hyuabot.backend.database.repository.SubwayStationNameRepository
import app.hyuabot.backend.database.repository.SubwayStationRepository
import app.hyuabot.backend.database.repository.SubwayTimetableRepository
import app.hyuabot.backend.subway.domain.BulkSubwayTimetableCreateRequest
import app.hyuabot.backend.subway.domain.BulkSubwayTimetableDeleteRequest
import app.hyuabot.backend.subway.domain.CreateSubwayRouteRequest
import app.hyuabot.backend.subway.domain.CreateSubwayStationRequest
import app.hyuabot.backend.subway.domain.SubwayTimetableKey
import app.hyuabot.backend.subway.domain.SubwayTimetableRequest
import app.hyuabot.backend.subway.domain.UpdateSubwayRouteRequest
import app.hyuabot.backend.subway.domain.UpdateSubwayStationRequest
import app.hyuabot.backend.subway.exception.DuplicateSubwayRouteException
import app.hyuabot.backend.subway.exception.DuplicateSubwayStationException
import app.hyuabot.backend.subway.exception.DuplicateSubwayTimetableException
import app.hyuabot.backend.subway.exception.SubwayRouteNotFoundException
import app.hyuabot.backend.subway.exception.SubwayStartStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTerminalStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTimetableNotFoundException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import kotlin.collections.emptyList
import app.hyuabot.backend.codegen.types.SubwayRoute as SubwayRouteDto
import app.hyuabot.backend.codegen.types.SubwayStation as SubwayStationDto
import app.hyuabot.backend.codegen.types.SubwayTimetable as SubwayTimetableDto

@Service
class SubwayService(
    private val nameRepository: SubwayStationNameRepository,
    private val stationRepository: SubwayStationRepository,
    private val routeRepository: SubwayRouteRepository,
    private val timetableRepository: SubwayTimetableRepository,
    private val realtimeRepository: SubwayRealtimeRepository,
    private val cacheManager: CacheManager,
) {
    fun getSubwayRoutes() = routeRepository.findAll()

    @CacheEvict(cacheNames = ["subwayStation", "subwayTimetable"], allEntries = true)
    fun createSubwayRoute(payload: CreateSubwayRouteRequest): SubwayRoute {
        routeRepository.findById(payload.id).orElse(null)?.let {
            throw DuplicateSubwayRouteException()
        }
        return routeRepository.save(
            SubwayRoute(
                id = payload.id,
                name = payload.name,
                station = mutableListOf(),
            ),
        )
    }

    fun getSubwayRouteById(id: Int): SubwayRoute = routeRepository.findById(id).orElseThrow { SubwayRouteNotFoundException() }

    @CacheEvict(cacheNames = ["subwayStation", "subwayTimetable"], allEntries = true)
    fun updateSubwayRoute(
        id: Int,
        payload: UpdateSubwayRouteRequest,
    ): SubwayRoute {
        val route = routeRepository.findById(id).orElseThrow { SubwayRouteNotFoundException() }
        route.let {
            it.name = payload.name
            return routeRepository.save(it)
        }
    }

    @CacheEvict(cacheNames = ["subwayStation", "subwayTimetable"], allEntries = true)
    @Transactional
    fun deleteSubwayRoute(id: Int) {
        val route = routeRepository.findById(id).orElseThrow { SubwayRouteNotFoundException() }
        timetableRepository.deleteAll(route.station.flatMap { it.timetable ?: emptyList() })
        realtimeRepository.deleteAll(route.station.flatMap { it.realtime ?: emptyList() })
        stationRepository.deleteAll(route.station)
        routeRepository.delete(route)
    }

    fun getAllStations() = stationRepository.findAll()

    fun getStations(stationIDList: List<String>): List<SubwayRouteStation> {
        if (stationIDList.isEmpty()) return emptyList()
        val stations = stationRepository.findByIdIn(stationIDList)
        if (stations.isEmpty()) return emptyList()
        val stationsById: Map<String, SubwayRouteStation> = stations.associateBy { it.id }
        return stationIDList.distinct().mapNotNull { stationsById[it] }
    }

    // Static station/route reference for the subway query skeleton (realtime/timetable/arrival
    // are filled by separate field resolvers). Cached as a DTO to avoid entity lazy-serialization.
    @Cacheable(cacheNames = ["subwayStation"], key = "#stationIDList")
    fun getStationViews(stationIDList: List<String>): List<SubwayStationDto> =
        getStations(stationIDList).map { station ->
            SubwayStationDto(
                stationID = station.id,
                name = station.name,
                order = station.order,
                minutes = station.cumulativeTime.toMinutes().toInt(),
                route =
                    SubwayRouteDto(
                        seq = station.route!!.id,
                        name = station.route!!.name,
                    ),
                realtime = emptyList(),
                timetable = emptyList(),
                arrival = emptyList(),
            )
        }

    @CacheEvict(cacheNames = ["subwayStation", "subwayTimetable"], allEntries = true)
    fun createStation(payload: CreateSubwayStationRequest): SubwayRouteStation {
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.cumulativeTime)) {
            throw DurationNotValidException()
        }
        stationRepository.findById(payload.id).orElse(null)?.let {
            throw DuplicateSubwayStationException()
        }
        nameRepository.findByName(payload.name) ?: nameRepository.save(SubwayStation(name = payload.name, mutableListOf()))
        return stationRepository.save(
            SubwayRouteStation(
                id = payload.id,
                routeID = payload.routeID,
                name = payload.name,
                order = payload.order,
                cumulativeTime = LocalDateTimeBuilder.convertStringToDuration(payload.cumulativeTime),
                route = null,
                stationName = null,
                realtime = mutableListOf(),
                timetable = mutableListOf(),
            ),
        )
    }

    fun getStationById(id: String): SubwayRouteStation = stationRepository.findById(id).orElseThrow { SubwayStationNotFoundException() }

    fun cleanUpUselessStationName(name: String) {
        nameRepository.findByName(name)!!.let { station ->
            if (station.subwayLine.isEmpty()) {
                nameRepository.delete(station)
            }
        }
    }

    @CacheEvict(cacheNames = ["subwayStation", "subwayTimetable"], allEntries = true)
    @Transactional
    fun updateStation(
        id: String,
        payload: UpdateSubwayStationRequest,
    ): SubwayRouteStation {
        val station = stationRepository.findById(id).orElseThrow { SubwayStationNotFoundException() }
        val oldStationName = station.name
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.cumulativeTime)) {
            throw DurationNotValidException()
        }
        station.let {
            it.routeID = payload.routeID
            it.name = payload.name
            it.order = payload.order
            it.cumulativeTime = LocalDateTimeBuilder.convertStringToDuration(payload.cumulativeTime)
        }
        if (oldStationName != payload.name) {
            cleanUpUselessStationName(oldStationName)
            nameRepository.findByName(payload.name) ?: nameRepository.save(SubwayStation(name = payload.name, mutableListOf()))
        }
        return stationRepository.save(station)
    }

    @CacheEvict(cacheNames = ["subwayStation", "subwayTimetable"], allEntries = true)
    @Transactional
    fun deleteStation(id: String) {
        val station = stationRepository.findById(id).orElseThrow { SubwayStationNotFoundException() }
        realtimeRepository.deleteAll(station.realtime ?: emptyList())
        timetableRepository.deleteAll(station.timetable ?: emptyList())
        stationRepository.delete(station)
        cleanUpUselessStationName(station.name)
    }

    fun getAllTimetables() = timetableRepository.findAll()

    fun getTimetablesByStationID(stationID: String) = timetableRepository.findByStationID(stationID)

    fun getTimetablesByStationIDAndDirection(
        stationID: String,
        direction: String,
    ) = timetableRepository.findByStationIDAndHeading(stationID, direction)

    fun getTimetablesByStationIDAndWeekday(
        stationID: String,
        weekday: String,
    ) = timetableRepository.findByStationIDAndWeekday(stationID, weekday)

    fun getTimetablesByStationIDAndDirectionAndWeekday(
        stationID: String,
        direction: String,
        weekday: String,
    ) = timetableRepository.findByStationIDAndHeadingAndWeekday(stationID, direction, weekday)

    fun getTimetableByStationIDAndSeq(
        stationID: String,
        seq: Int,
    ): SubwayTimetable =
        timetableRepository.findById(seq).orElseThrow { SubwayTimetableNotFoundException() }.let {
            if (it.stationID != stationID) {
                throw SubwayTimetableNotFoundException()
            }
            it
        }

    fun getTimetable(keys: Set<SubwayTimetableKey>): Map<SubwayTimetableKey, List<SubwayTimetable>> {
        if (keys.isEmpty()) return emptyMap()
        val stationIDList = keys.map { it.stationID }.distinct()
        val directions = keys.flatMap { it.directions }.distinct()
        val weekdays = keys.flatMap { it.weekdays }.distinct()
        if (directions.isEmpty() || weekdays.isEmpty()) return emptyMap()
        val grouped =
            timetableRepository
                .findByStationIdInAndHeadingInAndWeekdayIn(
                    stationIDList,
                    directions,
                    weekdays,
                ).groupBy {
                    Triple(
                        it.stationID,
                        it.heading,
                        it.weekday,
                    )
                }
        return keys.associateWith { key ->
            key.directions.distinct().flatMap { direction ->
                key.weekdays.distinct().flatMap { weekday ->
                    grouped[Triple(key.stationID, direction, weekday)] ?: emptyList()
                }
            }
        }
    }

    // Static timetable DTOs keyed by (station, directions, weekdays). Cross-request cached per key,
    // but cache misses are resolved with a SINGLE batched repository query (no per-key N+1).
    fun getTimetableViews(keys: Set<SubwayTimetableKey>): Map<SubwayTimetableKey, List<SubwayTimetableDto>> {
        if (keys.isEmpty()) return emptyMap()
        val cache = cacheManager.getCache("subwayTimetable")
        val result = LinkedHashMap<SubwayTimetableKey, List<SubwayTimetableDto>>()
        val misses = ArrayList<SubwayTimetableKey>()
        keys.forEach { key ->
            @Suppress("UNCHECKED_CAST")
            val cached = cache?.get(timetableCacheKey(key))?.get() as List<SubwayTimetableDto>?
            if (cached != null) result[key] = cached else misses.add(key)
        }
        if (misses.isNotEmpty()) {
            val stationIDs = misses.map { it.stationID }.distinct()
            val directions = misses.flatMap { it.directions }.distinct()
            val weekdays = misses.flatMap { it.weekdays }.distinct()
            val grouped: Map<Triple<String, String, String>, List<SubwayTimetable>> =
                if (directions.isEmpty() || weekdays.isEmpty()) {
                    emptyMap()
                } else {
                    timetableRepository
                        .findByStationIdInAndHeadingInAndWeekdayIn(stationIDs, directions, weekdays)
                        .groupBy { Triple(it.stationID, it.heading, it.weekday) }
                }
            misses.forEach { key ->
                val dtos =
                    key.directions.distinct().flatMap { direction ->
                        key.weekdays.distinct().flatMap { weekday ->
                            grouped[Triple(key.stationID, direction, weekday)].orEmpty().map { it.toTimetableDto() }
                        }
                    }
                result[key] = dtos
                cache?.put(timetableCacheKey(key), dtos)
            }
        }
        return result
    }

    private fun timetableCacheKey(key: SubwayTimetableKey) = "${key.stationID}:${key.directions.sorted()}:${key.weekdays.sorted()}"

    private fun SubwayTimetable.toTimetableDto() =
        SubwayTimetableDto(
            seq = seq!!,
            time = departureTime,
            weekday = weekday,
            direction = heading,
            origin = SubwayOriginTerminal(stationID = startStation!!.id, name = startStation!!.name),
            terminal = SubwayOriginTerminal(stationID = terminalStation!!.id, name = terminalStation!!.name),
        )

    @CacheEvict(cacheNames = ["subwayTimetable"], allEntries = true)
    fun createTimetable(
        stationID: String,
        payload: SubwayTimetableRequest,
    ): SubwayTimetable {
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.departureTime)) {
            throw LocalTimeNotValidException()
        }
        stationRepository.findById(stationID).orElseThrow { SubwayStationNotFoundException() }
        stationRepository.findById(payload.startStationID).orElseThrow { SubwayStartStationNotFoundException() }
        stationRepository.findById(payload.terminalStationID).orElseThrow { SubwayTerminalStationNotFoundException() }
        timetableRepository
            .findByStationIDAndHeadingAndWeekdayAndDepartureTime(
                stationID = stationID,
                heading = payload.direction,
                weekday = payload.weekday,
                departureTime = LocalTime.parse(payload.departureTime),
            )?.let {
                throw DuplicateSubwayTimetableException()
            }
        return timetableRepository.save(
            SubwayTimetable(
                stationID = stationID,
                startStationID = payload.startStationID,
                terminalStationID = payload.terminalStationID,
                departureTime = LocalTime.parse(payload.departureTime),
                weekday = payload.weekday,
                heading = payload.direction,
                station = null,
                startStation = null,
                terminalStation = null,
            ),
        )
    }

    @CacheEvict(cacheNames = ["subwayTimetable"], allEntries = true)
    fun updateTimetable(
        stationID: String,
        seq: Int,
        payload: SubwayTimetableRequest,
    ): SubwayTimetable {
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.departureTime)) {
            throw LocalTimeNotValidException()
        }
        val timetable = timetableRepository.findById(seq).orElseThrow { SubwayTimetableNotFoundException() }
        stationRepository.findById(stationID).orElseThrow { SubwayStationNotFoundException() }
        stationRepository.findById(payload.startStationID).orElseThrow { SubwayStartStationNotFoundException() }
        stationRepository.findById(payload.terminalStationID).orElseThrow { SubwayTerminalStationNotFoundException() }
        timetable.let {
            it.stationID = stationID
            it.startStationID = payload.startStationID
            it.terminalStationID = payload.terminalStationID
            it.departureTime = LocalTime.parse(payload.departureTime)
            it.weekday = payload.weekday
            it.heading = payload.direction
            return timetableRepository.save(it)
        }
    }

    @CacheEvict(cacheNames = ["subwayTimetable"], allEntries = true)
    fun deleteTimetable(
        stationID: String,
        seq: Int,
    ) {
        stationRepository.findById(stationID).orElseThrow { SubwayStationNotFoundException() }
        val timetable = timetableRepository.findById(seq).orElseThrow { SubwayTimetableNotFoundException() }
        if (timetable.stationID != stationID) {
            throw SubwayTimetableNotFoundException()
        }
        timetableRepository.delete(timetable)
    }

    @CacheEvict(cacheNames = ["subwayTimetable"], allEntries = true)
    @Transactional
    fun deleteTimetablesBulk(request: BulkSubwayTimetableDeleteRequest) {
        when {
            request.seqList != null -> timetableRepository.deleteAllBySeqIn(request.seqList)
            request.stationIDs != null ->
                when {
                    request.direction != null && request.weekday != null ->
                        timetableRepository.deleteAllByStationIDInAndHeadingAndWeekday(
                            request.stationIDs,
                            request.direction,
                            request.weekday,
                        )
                    request.direction != null ->
                        timetableRepository.deleteAllByStationIDInAndHeading(request.stationIDs, request.direction)
                    request.weekday != null ->
                        timetableRepository.deleteAllByStationIDInAndWeekday(request.stationIDs, request.weekday)
                    else -> timetableRepository.deleteAllByStationIDIn(request.stationIDs)
                }
            else -> throw IllegalArgumentException("seqList 또는 stationIDs 중 하나는 필수입니다.")
        }
    }

    @CacheEvict(cacheNames = ["subwayTimetable"], allEntries = true)
    @Transactional
    fun createTimetablesBulk(payloads: List<BulkSubwayTimetableCreateRequest>): List<SubwayTimetable> {
        val allStationIDs =
            (
                payloads.map { it.stationID } +
                    payloads.map { it.startStationID } +
                    payloads.map { it.terminalStationID }
            ).distinct()
        val stationsById = stationRepository.findByIdIn(allStationIDs).associateBy { it.id }

        val entities =
            payloads.map { payload ->
                if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.departureTime)) throw LocalTimeNotValidException()
                stationsById[payload.stationID] ?: throw SubwayStationNotFoundException()
                stationsById[payload.startStationID] ?: throw SubwayStartStationNotFoundException()
                stationsById[payload.terminalStationID] ?: throw SubwayTerminalStationNotFoundException()
                SubwayTimetable(
                    stationID = payload.stationID,
                    startStationID = payload.startStationID,
                    terminalStationID = payload.terminalStationID,
                    departureTime = LocalTime.parse(payload.departureTime),
                    weekday = payload.weekday,
                    heading = payload.direction,
                    station = null,
                    startStation = null,
                    terminalStation = null,
                )
            }
        return timetableRepository.saveAll(entities)
    }

    fun getRealtimeList() = realtimeRepository.findAll()

    fun getRealtimeList(
        stationID: String,
        directions: List<String>,
    ): List<SubwayRealtime> {
        if (directions.isEmpty()) return emptyList()
        return realtimeRepository.findByStationIDAndHeadingIn(stationID, directions)
    }

    private val serviceStartTime = LocalTime.of(4, 0)

    private fun toServiceSeconds(time: LocalTime): Long {
        val seconds = time.toSecondOfDay().toLong()
        val threshold = serviceStartTime.toSecondOfDay().toLong()
        return if (seconds >= threshold) seconds else seconds + 24L * 60 * 60
    }

    fun getArrival(
        stationID: String,
        directions: List<String>,
        weekday: String,
        limit: Int? = null,
        currentTime: LocalTime = LocalTime.now(LocalDateTimeBuilder.serviceTimezone),
    ): List<SubwayArrivalGroup> {
        if (directions.isEmpty()) return emptyList()
        val realtimeGroups =
            realtimeRepository
                .findByStationIDAndHeadingIn(stationID, directions)
                .sortedBy { it.remainingTime }
                .groupBy { it.heading }
        // Compute per-direction timetable search start in service-day seconds (no midnight wraparound)
        val currentServiceSecs = toServiceSeconds(currentTime)
        val timetableStartSvcSecsMap =
            directions.associateWith { direction ->
                val lastMinutes = realtimeGroups[direction].orEmpty().maxOfOrNull { it.remainingTime.toMinutes() }
                if (lastMinutes != null) currentServiceSecs + (lastMinutes + 5) * 60 else currentServiceSecs
            }
        // Fetch timetable entries with midnight-aware two-query approach
        val timetableEntries =
            if (currentTime.isBefore(serviceStartTime)) {
                // After midnight: only remaining after-midnight trains (currentTime to serviceStartTime)
                timetableRepository
                    .findByStationIDAndHeadingIsInAndWeekdayAndDepartureTimeAfter(
                        stationID = stationID,
                        heading = directions,
                        weekday = weekday,
                        departureTime = currentTime,
                    ).filter { it.departureTime.isBefore(serviceStartTime) }
            } else {
                // Normal hours: trains from now + after-midnight trains (00:00–serviceStartTime)
                val remaining =
                    timetableRepository.findByStationIDAndHeadingIsInAndWeekdayAndDepartureTimeAfter(
                        stationID = stationID,
                        heading = directions,
                        weekday = weekday,
                        departureTime = currentTime,
                    )
                val afterMidnight =
                    timetableRepository.findByStationIDAndHeadingIsInAndWeekdayAndDepartureTimeBefore(
                        stationID = stationID,
                        heading = directions,
                        weekday = weekday,
                        departureTime = serviceStartTime,
                    )
                remaining + afterMidnight
            }
        val timetableGroups = timetableEntries.groupBy { it.heading }
        return directions.map { direction ->
            val realtimeArrivals =
                realtimeGroups[direction].orEmpty().map { realtime ->
                    SubwayArrival(
                        minutes = realtime.remainingTime.toMinutes().toInt(),
                        origin = null,
                        terminal = realtime.terminalStation!!.toSubwayStation(),
                        isRealtime = true,
                        location = realtime.location,
                        stops = realtime.remainingStop,
                        trainNumber = realtime.trainNumber,
                        isExpress = realtime.isExpress,
                        isLast = realtime.isLast,
                        status = realtime.status,
                    )
                }
            val startSvcSecs = timetableStartSvcSecsMap[direction]!!
            val timetableArrivals =
                timetableGroups[direction]
                    .orEmpty()
                    .filter { it.startStationID == stationID || toServiceSeconds(it.departureTime) > startSvcSecs }
                    .map { timetable ->
                        SubwayArrival(
                            minutes = ((toServiceSeconds(timetable.departureTime) - currentServiceSecs) / 60).toInt(),
                            origin = timetable.startStation?.toSubwayStation(),
                            terminal = timetable.terminalStation!!.toSubwayStation(),
                            isRealtime = false,
                            location = null,
                            stops = null,
                            trainNumber = null,
                            isExpress = null,
                            isLast = null,
                            status = null,
                        )
                    }
            val allArrivals = (realtimeArrivals + timetableArrivals).sortedBy { it.minutes }
            SubwayArrivalGroup(
                direction = direction,
                entries = if (limit != null) allArrivals.take(limit) else allArrivals,
            )
        }
    }

    private fun SubwayRouteStation.toSubwayStation() =
        SubwayOriginTerminal(
            stationID = id,
            name = name,
        )
}
