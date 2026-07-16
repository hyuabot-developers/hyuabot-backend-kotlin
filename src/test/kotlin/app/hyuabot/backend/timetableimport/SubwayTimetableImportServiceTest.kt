package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.database.repository.SubwayStationRepository
import app.hyuabot.backend.database.repository.SubwayTimetableRepository
import app.hyuabot.backend.subway.domain.BulkSubwayTimetableCreateRequest
import app.hyuabot.backend.timetableimport.domain.SubwayTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.SubwayTimetableImportSnapshot
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

class SubwayTimetableImportServiceTest {
    private val stationRepository = mock<SubwayStationRepository>()
    private val timetableRepository = mock<SubwayTimetableRepository>()
    private val store = mock<TimetableImportStore>()
    private val service = SubwayTimetableImportService(stationRepository, timetableRepository, store)

    @Test
    fun `preview reports creates updates deletes and unchanged rows`() {
        val current =
            listOf(
                entity("A", "OLD", "T1", "05:00:00"),
                entity("A", "S2", "T2", "06:00:00"),
                entity("A", "S3", "T3", "07:00:00"),
            )
        val request =
            SubwayTimetableImportRequest(
                stationIDs = listOf("A", "A"),
                entries =
                    listOf(
                        entry("A", "NEW", "T1", "05:00:00"),
                        entry("A", "S3", "T3", "07:00:00"),
                        entry("A", "S4", "T4", "08:00:00"),
                    ),
            )
        whenever(stationRepository.findByIdIn(any())).thenReturn(stations("A", "OLD", "NEW", "T1", "S2", "T2", "S3", "T3", "S4", "T4"))
        whenever(timetableRepository.findByStationIDIn(listOf("A"))).thenReturn(current)
        whenever(store.save(any(), any())).thenReturn(StoredTimetableImport("preview", ZonedDateTime.now().plusMinutes(15)))

        val result = service.preview(request)

        assertEquals("preview", result.previewID)
        assertEquals(1, result.createCount)
        assertEquals(1, result.updateCount)
        assertEquals(1, result.deleteCount)
        assertEquals(1, result.unchangedCount)
        assertEquals(3, result.sampleChanges.size)
        assertEquals(1, result.warnings.size)
        verify(store).save(any(), any<SubwayTimetableImportSnapshot>())
    }

    @Test
    fun `preview returns all validation problems without storing data`() {
        val invalid =
            SubwayTimetableImportRequest(
                stationIDs = emptyList(),
                entries =
                    listOf(
                        entry("missing", "missing-start", "missing-end", "bad", "holiday", "side"),
                        entry("missing", "missing-start", "missing-end", "bad", "holiday", "side"),
                        entry("missing", "missing-start", "missing-end", "05:00:00"),
                        entry("missing", "missing-start", "missing-end", "05:00:00"),
                    ),
            )
        whenever(stationRepository.findByIdIn(any())).thenReturn(emptyList())

        val result = service.preview(invalid)

        assertNull(result.previewID)
        assertEquals(0, result.createCount)
        assert(
            result.errors
                .map {
                    it.code
                }.containsAll(
                    listOf(
                        "EMPTY_SCOPE",
                        "OUT_OF_SCOPE",
                        "INVALID_TIME",
                        "INVALID_WEEKDAY",
                        "INVALID_DIRECTION",
                        "STATION_NOT_FOUND",
                    ),
                ),
        )
    }

    @Test
    fun `empty imports and imports without deletions are represented safely`() {
        whenever(stationRepository.findByIdIn(emptyList())).thenReturn(emptyList())
        assert(service.preview(SubwayTimetableImportRequest(emptyList(), emptyList())).errors.any { it.code == "EMPTY_IMPORT" })

        val request = SubwayTimetableImportRequest(listOf("A"), listOf(entry("A", "S", "T", "05:00:00")))
        whenever(stationRepository.findByIdIn(any())).thenReturn(stations("A", "S", "T"))
        whenever(timetableRepository.findByStationIDIn(listOf("A"))).thenReturn(emptyList())
        whenever(store.save(any(), any())).thenReturn(StoredTimetableImport("preview", ZonedDateTime.now().plusMinutes(15)))
        assert(service.preview(request).warnings.isEmpty())
    }

    @Test
    fun `apply replaces the scoped timetable and consumes the preview`() {
        val request = SubwayTimetableImportRequest(listOf("A"), listOf(entry("A", "S", "T", "05:00:00")))
        val current = listOf(entity("A", "OLD", "T", "04:00:00"))
        val snapshot = SubwayTimetableImportSnapshot(request, fingerprint(current), TimetableImportApplyResponse(1, 0, 1, 0))
        whenever(store.load("subway", "preview", SubwayTimetableImportSnapshot::class.java)).thenReturn(snapshot)
        whenever(timetableRepository.findByStationIDInForUpdate(listOf("A"))).thenReturn(current)

        assertEquals(snapshot.result, service.apply("preview"))

        verify(timetableRepository).deleteAllByStationIDIn(listOf("A"))
        verify(timetableRepository).flush()
        val captor = argumentCaptor<List<SubwayTimetable>>()
        verify(timetableRepository).saveAll(captor.capture())
        assertEquals(LocalTime.of(5, 0), captor.firstValue.single().departureTime)
        verify(store).consume("subway", "preview")
        verify(store).release("subway", "preview")
    }

    @Test
    fun `apply rejects stale previews and always releases the lock`() {
        val snapshot =
            SubwayTimetableImportSnapshot(
                SubwayTimetableImportRequest(listOf("A"), emptyList()),
                "old",
                TimetableImportApplyResponse(0, 0, 0, 0),
            )
        whenever(store.load("subway", "preview", SubwayTimetableImportSnapshot::class.java)).thenReturn(snapshot)
        whenever(timetableRepository.findByStationIDInForUpdate(listOf("A"))).thenReturn(emptyList())

        assertEquals("PREVIEW_STALE", assertThrows(TimetableImportException::class.java) { service.apply("preview") }.code)
        verify(store).release("subway", "preview")
    }

    private fun entry(
        station: String,
        start: String,
        terminal: String,
        time: String,
        weekday: String = "weekdays",
        direction: String = "up",
    ) = BulkSubwayTimetableCreateRequest(station, start, terminal, time, weekday, direction)

    private fun entity(
        station: String,
        start: String,
        terminal: String,
        time: String,
    ) = SubwayTimetable(null, station, start, terminal, LocalTime.parse(time), "weekdays", "up", null, null, null)

    private fun stations(vararg ids: String) =
        ids.map { id -> SubwayRouteStation(id, 1, id, 1, Duration.ZERO, null, null, mutableListOf(), mutableListOf()) }

    private fun fingerprint(values: List<SubwayTimetable>) =
        TimetableImportSupport.fingerprint(
            values.map {
                "${it.stationID}|${it.weekday}|${it.heading}|${it.departureTime}|${it.startStationID}|${it.terminalStationID}"
            },
        )
}
