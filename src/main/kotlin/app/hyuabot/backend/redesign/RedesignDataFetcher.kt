package app.hyuabot.backend.redesign

import app.hyuabot.backend.codegen.types.SubwayStation
import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.database.repository.SubwayStationFacilityRepository
import app.hyuabot.backend.database.repository.SubwayTimetableRepository
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime

@DgsComponent
class RedesignDataFetcher(
    private val facilityRepository: SubwayStationFacilityRepository,
    private val timetableRepository: SubwayTimetableRepository,
    private val publicHolidayService: PublicHolidayService,
    private val clock: Clock = Clock.systemUTC(),
) {
    @DgsData(parentType = "SubwayStation", field = "facilities")
    fun facilities(dfe: DgsDataFetchingEnvironment): List<Map<String, Any?>> {
        val station = dfe.getSource<SubwayStation>()!!
        return facilityRepository.findByStationIDOrderByFacilityTypeAscSortOrderAsc(station.stationID).map {
            mapOf(
                "type" to it.facilityType,
                "order" to it.sortOrder,
                "exitNumber" to it.exitNumber,
                "fromPlace" to it.fromPlace,
                "toPlace" to it.toPlace,
                "descriptionKorean" to it.descriptionKorean,
                "descriptionEnglish" to it.descriptionEnglish,
                "source" to it.source,
            )
        }
    }

    @DgsData(parentType = "SubwayStation", field = "firstLast")
    fun firstLast(dfe: DgsDataFetchingEnvironment): List<Map<String, Any?>> {
        val station = dfe.getSource<SubwayStation>()!!
        val now = ZonedDateTime.now(clock.withZone(LocalDateTimeBuilder.serviceTimezone))
        val serviceDate =
            if (now.toLocalTime().isBefore(
                    java.time.LocalTime.of(4, 0),
                )
            ) {
                now.toLocalDate().minusDays(1)
            } else {
                now.toLocalDate()
            }
        val weekday =
            if (serviceDate.dayOfWeek.value >= 6 ||
                publicHolidayService.findPublicHoliday(serviceDate) != null
            ) {
                "weekends"
            } else {
                "weekdays"
            }
        return timetableRepository
            .findByStationID(station.stationID)
            .filter { it.weekday == weekday }
            .groupBy { it.heading to it.weekday }
            .map { (key, entries) ->
                val (first, last) = subwayFirstAndLast(entries)
                mapOf(
                    "direction" to
                        when (key.first) {
                            "0" -> "up"
                            "1" -> "down"
                            else -> key.first
                        },
                    "weekday" to key.second,
                    "first" to first,
                    "last" to last,
                )
            }.sortedWith(compareBy<Map<String, Any?>> { it["direction"].toString() }.thenBy { it["weekday"].toString() })
    }
}

internal fun subwayFirstAndLast(entries: List<SubwayTimetable>): Pair<LocalTime?, LocalTime?> =
    entries.minOfOrNull { it.departureTime } to entries.maxOfOrNull { it.departureTime }
