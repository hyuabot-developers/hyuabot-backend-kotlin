package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.database.entity.ShuttlePeriodType
import app.hyuabot.backend.database.entity.ShuttleRoute
import app.hyuabot.backend.database.entity.ShuttleTimetable
import app.hyuabot.backend.database.repository.ShuttlePeriodTypeRepository
import app.hyuabot.backend.database.repository.ShuttleRouteRepository
import app.hyuabot.backend.database.repository.ShuttleTimetableRepository
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportEntry
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportSnapshot
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
import java.time.LocalTime
import java.time.ZonedDateTime

class ShuttleTimetableImportServiceTest {
    private val routeRepository = mock<ShuttleRouteRepository>()
    private val periodRepository = mock<ShuttlePeriodTypeRepository>()
    private val timetableRepository = mock<ShuttleTimetableRepository>()
    private val store = mock<TimetableImportStore>()
    private val service = ShuttleTimetableImportService(routeRepository, periodRepository, timetableRepository, store)

    @Test
    fun `preview reports creates deletes unchanged rows and warnings`() {
        val current = listOf(entity("A", "semester", true, "05:00:00"), entity("A", "semester", true, "06:00:00"))
        val request =
            ShuttleTimetableImportRequest(
                listOf("A", "A"),
                listOf(entry("A", "semester", true, "05:00:00"), entry("A", "semester", false, "07:00:00")),
            )
        stubReferences("A", "semester")
        whenever(timetableRepository.findByRouteNameIn(listOf("A"))).thenReturn(current)
        whenever(store.save(any(), any())).thenReturn(StoredTimetableImport("preview", ZonedDateTime.now().plusMinutes(15)))

        val result = service.preview(request)

        assertEquals("preview", result.previewID)
        assertEquals(1, result.createCount)
        assertEquals(0, result.updateCount)
        assertEquals(1, result.deleteCount)
        assertEquals(1, result.unchangedCount)
        assertEquals(2, result.sampleChanges.size)
        assertEquals(1, result.warnings.size)
        verify(store).save(any(), any<ShuttleTimetableImportSnapshot>())
    }

    @Test
    fun `preview reports invalid route period time scope duplicate and empty input`() {
        whenever(routeRepository.findAllById(any<List<String>>())).thenReturn(emptyList())
        whenever(periodRepository.findAllById(any<List<String>>())).thenReturn(emptyList())
        val invalidEntry = entry("missing", "unknown", true, "bad")
        val duplicateEntry = entry("missing", "unknown", false, "05:00:00")
        val weekdayDuplicate = entry("missing", "unknown", true, "06:00:00")
        val result =
            service.preview(
                ShuttleTimetableImportRequest(
                    emptyList(),
                    listOf(invalidEntry, duplicateEntry, duplicateEntry, weekdayDuplicate, weekdayDuplicate),
                ),
            )

        assertNull(result.previewID)
        assert(
            result.errors
                .map {
                    it.code
                }.containsAll(listOf("EMPTY_SCOPE", "OUT_OF_SCOPE", "ROUTE_NOT_FOUND", "PERIOD_NOT_FOUND", "INVALID_TIME")),
        )

        whenever(routeRepository.findAllById(emptyList<String>())).thenReturn(emptyList())
        whenever(periodRepository.findAllById(emptyList<String>())).thenReturn(emptyList())
        assert(service.preview(ShuttleTimetableImportRequest(emptyList(), emptyList())).errors.any { it.code == "EMPTY_IMPORT" })
    }

    @Test
    fun `preview without deletions has no warning`() {
        stubReferences("A", "semester")
        whenever(timetableRepository.findByRouteNameIn(listOf("A"))).thenReturn(emptyList())
        whenever(store.save(any(), any())).thenReturn(StoredTimetableImport("preview", ZonedDateTime.now().plusMinutes(15)))
        val result = service.preview(ShuttleTimetableImportRequest(listOf("A"), listOf(entry("A", "semester", true, "05:00:00"))))
        assert(result.warnings.isEmpty())
    }

    @Test
    fun `apply atomically replaces routes and consumes preview`() {
        val request = ShuttleTimetableImportRequest(listOf("A"), listOf(entry("A", "semester", true, "05:00:00")))
        val current = listOf(entity("A", "semester", true, "04:00:00"))
        val snapshot = ShuttleTimetableImportSnapshot(request, fingerprint(current), TimetableImportApplyResponse(1, 0, 1, 0))
        whenever(store.load("shuttle", "preview", ShuttleTimetableImportSnapshot::class.java)).thenReturn(snapshot)
        whenever(timetableRepository.findByRouteNameInForUpdate(listOf("A"))).thenReturn(current)

        assertEquals(snapshot.result, service.apply("preview"))

        verify(timetableRepository).deleteAllByRouteNameIn(listOf("A"))
        verify(timetableRepository).flush()
        val captor = argumentCaptor<List<ShuttleTimetable>>()
        verify(timetableRepository).saveAll(captor.capture())
        assertEquals("A", captor.firstValue.single().routeName)
        verify(store).consume("shuttle", "preview")
        verify(store).release("shuttle", "preview")
    }

    @Test
    fun `apply rejects stale previews and releases lock`() {
        val snapshot =
            ShuttleTimetableImportSnapshot(
                ShuttleTimetableImportRequest(listOf("A"), emptyList()),
                "old",
                TimetableImportApplyResponse(0, 0, 0, 0),
            )
        whenever(store.load("shuttle", "preview", ShuttleTimetableImportSnapshot::class.java)).thenReturn(snapshot)
        whenever(timetableRepository.findByRouteNameInForUpdate(listOf("A"))).thenReturn(emptyList())
        assertEquals("PREVIEW_STALE", assertThrows(TimetableImportException::class.java) { service.apply("preview") }.code)
        verify(store).release("shuttle", "preview")
    }

    private fun stubReferences(
        route: String,
        period: String,
    ) {
        whenever(routeRepository.findAllById(any<List<String>>())).thenReturn(
            listOf(
                ShuttleRoute(
                    name = route,
                    descriptionKorean = "ko",
                    descriptionEnglish = "en",
                    tag = "tag",
                    startStopID = "start",
                    endStopID = "end",
                    timetable = mutableListOf(),
                    stop = mutableListOf(),
                    startStop = null,
                    endStop = null,
                ),
            ),
        )
        whenever(periodRepository.findAllById(any<List<String>>())).thenReturn(listOf(ShuttlePeriodType(period, mutableListOf())))
    }

    private fun entry(
        route: String,
        period: String,
        weekday: Boolean,
        time: String,
    ) = ShuttleTimetableImportEntry(route, period, weekday, time)

    private fun entity(
        route: String,
        period: String,
        weekday: Boolean,
        time: String,
    ) = ShuttleTimetable(null, period, weekday, route, LocalTime.parse(time), null)

    private fun fingerprint(values: List<ShuttleTimetable>) =
        TimetableImportSupport.fingerprint(values.map { "${it.routeName}|${it.periodType}|${it.weekday}|${it.departureTime}" })
}
