package app.hyuabot.backend.redesign

import app.hyuabot.backend.codegen.types.SubwayStation
import app.hyuabot.backend.database.repository.PublicHolidayRepository
import app.hyuabot.backend.database.repository.SpecialDayRepository
import app.hyuabot.backend.database.repository.SubwayAlertRepository
import app.hyuabot.backend.database.repository.SubwayStationFacilityRepository
import app.hyuabot.backend.database.repository.SubwayTimetableRepository
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import java.time.LocalDate
import java.time.ZonedDateTime

@DgsComponent
class RedesignDataFetcher(
    private val specialDayRepository: SpecialDayRepository,
    private val facilityRepository: SubwayStationFacilityRepository,
    private val timetableRepository: SubwayTimetableRepository,
    private val alertRepository: SubwayAlertRepository,
    private val publicHolidayRepository: PublicHolidayRepository,
    private val publicHolidayService: PublicHolidayService,
) {
    @DgsQuery
    fun specialDays(
        @InputArgument start: LocalDate,
        @InputArgument end: LocalDate,
    ): List<Map<String, Any?>> {
        if (start.isAfter(end)) return emptyList()
        val special =
            specialDayRepository.findByDayDateBetweenOrderByDayDateAscDayNameAsc(start, end).map {
                mapOf("date" to it.date, "name" to it.name, "kind" to it.kind, "isHoliday" to it.isHoliday, "source" to it.source)
            }
        val holidays =
            publicHolidayRepository
                .findByDateBetween(start, end)
                .asSequence()
                .map { mapOf("date" to it.date, "name" to it.name, "kind" to "holiday", "isHoliday" to true, "source" to it.source) }
                .toList()
        return (special + holidays)
            .distinctBy { it["date"] to it["name"] }
            .sortedWith(compareBy<Map<String, Any?>> { it["date"].toString() }.thenBy { it["name"].toString() })
    }

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
        val now = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
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
                mapOf(
                    "direction" to
                        when (key.first) {
                            "0" -> "up"
                            "1" -> "down"
                            else -> key.first
                        },
                    "weekday" to key.second,
                    "first" to entries.minOfOrNull { it.departureTime },
                    "last" to entries.maxOfOrNull { it.departureTime },
                )
            }.sortedWith(compareBy<Map<String, Any?>> { it["direction"].toString() }.thenBy { it["weekday"].toString() })
    }

    @DgsData(parentType = "SubwayStation", field = "alerts")
    fun alerts(dfe: DgsDataFetchingEnvironment): List<Map<String, Any?>> {
        val station = dfe.getSource<SubwayStation>()!!
        val now = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        return alertRepository
            .findAll()
            .asSequence()
            .filter {
                (it.routeID == null || it.routeID == station.route.seq) &&
                    (it.startsAt == null || !it.startsAt!!.isAfter(now)) &&
                    (it.endsAt == null || it.endsAt!!.isAfter(now))
            }.sortedByDescending { it.startsAt }
            .map {
                mapOf(
                    "id" to it.id,
                    "routeID" to it.routeID,
                    "title" to it.title,
                    "content" to it.content,
                    "startsAt" to it.startsAt,
                    "endsAt" to it.endsAt,
                    "source" to it.source,
                )
            }.toList()
    }
}
