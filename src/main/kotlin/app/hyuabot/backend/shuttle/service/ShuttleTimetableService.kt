package app.hyuabot.backend.shuttle.service

import app.hyuabot.backend.database.entity.ShuttleTimetableView
import app.hyuabot.backend.database.repository.ShuttleTimetableViewRepository
import app.hyuabot.backend.shuttle.domain.ShuttleArrivalItem
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableKey
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableResult
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableViewItem
import org.springframework.stereotype.Service

@Service
class ShuttleTimetableService(
    private val shuttleTimetableViewRepository: ShuttleTimetableViewRepository,
) {
    fun getShuttleTimetable(): List<ShuttleTimetableView> = shuttleTimetableViewRepository.findAll().sortedBy { it.seq }

    fun getShuttleTimetableBatch(keys: Set<ShuttleTimetableKey>): Map<ShuttleTimetableKey, ShuttleTimetableResult> {
        if (keys.isEmpty()) return emptyMap()
        val stops = keys.map { it.stop }.distinct()
        val periods = keys.flatMap { it.periods }.distinct()
        val weekdays = keys.flatMap { it.weekdays }.distinct()
        val routes =
            keys
                .mapNotNull { it.routes }
                .flatten()
                .distinct()
        val tags = keys.mapNotNull { it.tags }.flatten().distinct()
        val destinations = keys.mapNotNull { it.destinations }.flatten().distinct()
        if (periods.isEmpty() || weekdays.isEmpty()) {
            return keys.associateWith {
                ShuttleTimetableResult(
                    order = emptyList(),
                    destination = emptyMap(),
                )
            }
        }
        val allRows =
            if (routes.isNotEmpty() && tags.isNotEmpty() && destinations.isNotEmpty()) {
                shuttleTimetableViewRepository
                    .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndRouteTagIsInAndDestinationGroupIsIn(
                        periods,
                        stops,
                        weekdays,
                        routes,
                        tags,
                        destinations,
                    )
            } else if (routes.isNotEmpty() && tags.isNotEmpty()) {
                shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndRouteTagIsIn(
                    periods,
                    stops,
                    weekdays,
                    routes,
                    tags,
                )
            } else if (routes.isNotEmpty() && destinations.isNotEmpty()) {
                shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndDestinationGroupIsIn(
                    periods,
                    stops,
                    weekdays,
                    routes,
                    destinations,
                )
            } else if (tags.isNotEmpty() && destinations.isNotEmpty()) {
                shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsInAndDestinationGroupIsIn(
                    periods,
                    stops,
                    weekdays,
                    tags,
                    destinations,
                )
            } else if (routes.isNotEmpty()) {
                shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsIn(
                    periods,
                    stops,
                    weekdays,
                    routes,
                )
            } else if (tags.isNotEmpty()) {
                shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsIn(
                    periods,
                    stops,
                    weekdays,
                    tags,
                )
            } else if (destinations.isNotEmpty()) {
                shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndDestinationGroupIsIn(
                    periods,
                    stops,
                    weekdays,
                    destinations,
                )
            } else {
                shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                    periods,
                    stops,
                    weekdays,
                )
            }
        val groupedBySeqAndGroup = allRows.groupBy { it.seq to it.destinationGroup }
        val groupedBySeq = allRows.groupBy { it.seq }
        return keys.associateWith { key ->
            val relevantKeys =
                allRows
                    .filter {
                        it.stopName == key.stop &&
                            (key.after == null || it.departureTime > key.after) &&
                            (key.routes == null || key.routes.contains(it.routeName)) &&
                            (key.tags == null || key.tags.contains(it.routeTag)) &&
                            (key.destinations == null || key.destinations.contains(it.destinationGroup))
                    }.map { it.seq to it.destinationGroup }
                    .toSet()
            if (relevantKeys.isEmpty()) {
                return@associateWith ShuttleTimetableResult(
                    order = emptyList(),
                    destination = emptyMap(),
                )
            }
            val entries =
                relevantKeys
                    .map { seqKey ->
                        val stops = groupedBySeqAndGroup[seqKey].orEmpty().sortedBy { it.departureTime }
                        val viaStops = groupedBySeq[seqKey.first].orEmpty().sortedBy { it.departureTime }
                        val main = stops.first { it.stopName == key.stop }
                        ShuttleTimetableViewItem(
                            seq = seqKey.first,
                            routeName = main.routeName,
                            routeTag = main.routeTag,
                            period = main.periodType,
                            weekday = main.weekday,
                            time = main.departureTime,
                            group = main.destinationGroup,
                            stops =
                                viaStops.map { stop ->
                                    ShuttleArrivalItem(
                                        stop = stop.stopName,
                                        time = stop.departureTime,
                                    )
                                },
                        )
                    }.sortedBy { it.time }
            val order = if (key.limit.order != null) entries.take(key.limit.order) else entries
            val destination =
                entries
                    .groupBy { it.group }
                    .mapValues { (_, v) ->
                        if (key.limit.destination != null) v.take(key.limit.destination) else v
                    }
            ShuttleTimetableResult(
                order = order,
                destination = destination,
            )
        }
    }
}
