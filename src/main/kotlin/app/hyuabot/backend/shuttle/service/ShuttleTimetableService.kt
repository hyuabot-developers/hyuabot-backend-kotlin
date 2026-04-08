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
        val allPeriods = keys.flatMap { it.periods }.distinct()
        val allWeekdays = keys.flatMap { it.weekdays }.distinct()
        if (allPeriods.isEmpty() || allWeekdays.isEmpty()) {
            return keys.associateWith {
                ShuttleTimetableResult(
                    order = emptyList(),
                    destination = emptyMap(),
                )
            }
        }
        // Partition keys by their filter combination so that each partition issues its own
        // DB query. This prevents a filter from one key from excluding rows that another key
        // (with a different or absent filter) legitimately needs.
        val allRows: List<ShuttleTimetableView> =
            keys
                .groupBy { Triple(it.routes, it.tags, it.destinations) }
                .flatMap { (combo, keysInGroup) ->
                    val groupStops = keysInGroup.map { it.stop }.distinct()
                    val routes = combo.first?.toList()
                    val tags = combo.second?.toList()
                    val destinations = combo.third?.toList()
                    when {
                        routes != null && tags != null && destinations != null -> {
                            shuttleTimetableViewRepository
                                .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndRouteTagIsInAndDestinationGroupIsIn(
                                    allPeriods,
                                    groupStops,
                                    allWeekdays,
                                    routes,
                                    tags,
                                    destinations,
                                )
                        }

                        routes != null && tags != null -> {
                            shuttleTimetableViewRepository
                                .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndRouteTagIsIn(
                                    allPeriods,
                                    groupStops,
                                    allWeekdays,
                                    routes,
                                    tags,
                                )
                        }

                        routes != null && destinations != null -> {
                            shuttleTimetableViewRepository
                                .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndDestinationGroupIsIn(
                                    allPeriods,
                                    groupStops,
                                    allWeekdays,
                                    routes,
                                    destinations,
                                )
                        }

                        tags != null && destinations != null -> {
                            shuttleTimetableViewRepository
                                .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsInAndDestinationGroupIsIn(
                                    allPeriods,
                                    groupStops,
                                    allWeekdays,
                                    tags,
                                    destinations,
                                )
                        }

                        routes != null -> {
                            shuttleTimetableViewRepository
                                .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsIn(
                                    allPeriods,
                                    groupStops,
                                    allWeekdays,
                                    routes,
                                )
                        }

                        tags != null -> {
                            shuttleTimetableViewRepository
                                .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsIn(
                                    allPeriods,
                                    groupStops,
                                    allWeekdays,
                                    tags,
                                )
                        }

                        destinations != null -> {
                            shuttleTimetableViewRepository
                                .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndDestinationGroupIsIn(
                                    allPeriods,
                                    groupStops,
                                    allWeekdays,
                                    destinations,
                                )
                        }

                        else -> {
                            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                                allPeriods,
                                groupStops,
                                allWeekdays,
                            )
                        }
                    }
                }.distinct()
        val allSequences = allRows.map { it.seq }.distinct()
        val viaRows: List<ShuttleTimetableView> =
            if (allSequences.isEmpty()) {
                emptyList()
            } else {
                shuttleTimetableViewRepository
                    .findBySeqIn(
                        allSequences,
                    ).distinct()
            }
        val allRowsGroupedBySeqAndGroup = allRows.groupBy { it.seq to it.destinationGroup }
        val viaRowsGroupedBySeq = viaRows.groupBy { it.seq }
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
                        val stops = allRowsGroupedBySeqAndGroup[seqKey].orEmpty().sortedBy { it.departureTime }
                        val viaStops =
                            viaRowsGroupedBySeq[seqKey.first]
                                .orEmpty()
                                .distinctBy {
                                    it.stopName to it.departureTime
                                }.sortedBy { it.departureTime }
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
