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
        if (periods.isEmpty() || weekdays.isEmpty()) {
            return keys.associateWith {
                ShuttleTimetableResult(
                    order = emptyList(),
                    destination = emptyMap(),
                )
            }
        }
        val allRows =
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = periods,
                weekdays = weekdays,
                stops = stops,
            )
        val groupedBySeqAndGroup = allRows.groupBy { it.seq to it.destinationGroup }
        val groupedBySeq = allRows.groupBy { it.seq }
        return keys.associateWith { key ->
            val relevantKeys =
                allRows
                    .filter { it.stopName == key.stop }
                    .filter { key.after == null || it.departureTime > key.after }
                    .map { it.seq to it.destinationGroup }
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
