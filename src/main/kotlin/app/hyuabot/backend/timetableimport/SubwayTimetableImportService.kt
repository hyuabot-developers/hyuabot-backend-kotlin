package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.database.repository.SubwayStationRepository
import app.hyuabot.backend.database.repository.SubwayTimetableRepository
import app.hyuabot.backend.subway.domain.BulkSubwayTimetableCreateRequest
import app.hyuabot.backend.timetableimport.domain.SubwayTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.SubwayTimetableImportSnapshot
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyResponse
import app.hyuabot.backend.timetableimport.domain.TimetableImportChange
import app.hyuabot.backend.timetableimport.domain.TimetableImportIssue
import app.hyuabot.backend.timetableimport.domain.TimetableImportPreviewResponse
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
class SubwayTimetableImportService(
    private val stationRepository: SubwayStationRepository,
    private val timetableRepository: SubwayTimetableRepository,
    private val store: TimetableImportStore,
) {
    fun preview(request: SubwayTimetableImportRequest): TimetableImportPreviewResponse {
        val scope = request.stationIDs.distinct()
        val errors = validate(request, scope)
        if (errors.isNotEmpty()) return emptyPreview(errors)

        val current = timetableRepository.findByStationIDIn(scope)
        val currentByKey = current.associateBy(::key)
        val incomingByKey = request.entries.associateBy(::key)
        val createKeys = incomingByKey.keys - currentByKey.keys
        val deleteKeys = currentByKey.keys - incomingByKey.keys
        val updateKeys =
            (incomingByKey.keys intersect currentByKey.keys)
                .filter { timetableKey ->
                    val incoming = incomingByKey.getValue(timetableKey)
                    val existing = currentByKey.getValue(timetableKey)
                    incoming.startStationID != existing.startStationID || incoming.terminalStationID != existing.terminalStationID
                }.toSet()
        val unchangedCount = incomingByKey.keys.intersect(currentByKey.keys).size - updateKeys.size
        val result =
            TimetableImportApplyResponse(
                createCount = createKeys.size,
                updateCount = updateKeys.size,
                deleteCount = deleteKeys.size,
                unchangedCount = unchangedCount,
            )
        val snapshot =
            SubwayTimetableImportSnapshot(
                request = request.copy(stationIDs = scope),
                fingerprint = fingerprint(current),
                result = result,
            )
        val stored = store.save(TYPE, snapshot)
        val changes =
            buildList {
                createKeys.take(SAMPLE_LIMIT).forEach { timetableKey ->
                    add(change("CREATE", timetableKey, null, incomingByKey.getValue(timetableKey).description()))
                }
                updateKeys.take(remainingCapacity()).forEach { timetableKey ->
                    add(
                        change(
                            "UPDATE",
                            timetableKey,
                            currentByKey.getValue(timetableKey).description(),
                            incomingByKey.getValue(timetableKey).description(),
                        ),
                    )
                }
                deleteKeys.take(remainingCapacity()).forEach { timetableKey ->
                    add(change("DELETE", timetableKey, currentByKey.getValue(timetableKey).description(), null))
                }
            }
        val warnings =
            if (deleteKeys.isEmpty()) {
                emptyList()
            } else {
                listOf(TimetableImportIssue(null, "DELETE_EXISTING", "기존 시간표 ${deleteKeys.size}건이 삭제됩니다."))
            }
        return TimetableImportPreviewResponse(
            previewID = stored.id,
            expiresAt = stored.expiresAt.toString(),
            createCount = result.createCount,
            updateCount = result.updateCount,
            deleteCount = result.deleteCount,
            unchangedCount = result.unchangedCount,
            errors = emptyList(),
            warnings = warnings,
            sampleChanges = changes,
        )
    }

    @CacheEvict(cacheNames = ["subwayTimetable"], allEntries = true)
    @Transactional
    fun apply(previewID: String): TimetableImportApplyResponse {
        store.acquire(TYPE, previewID)
        try {
            val snapshot = store.load(TYPE, previewID, SubwayTimetableImportSnapshot::class.java)
            val current = timetableRepository.findByStationIDInForUpdate(snapshot.request.stationIDs)
            if (fingerprint(current) != snapshot.fingerprint) throw TimetableImportException("PREVIEW_STALE")
            timetableRepository.deleteAllByStationIDIn(snapshot.request.stationIDs)
            timetableRepository.flush()
            timetableRepository.saveAll(snapshot.request.entries.map(::toEntity))
            store.consume(TYPE, previewID)
            return snapshot.result
        } finally {
            store.release(TYPE, previewID)
        }
    }

    private fun validate(
        request: SubwayTimetableImportRequest,
        scope: List<String>,
    ): List<TimetableImportIssue> {
        val issues = mutableListOf<TimetableImportIssue>()
        if (scope.isEmpty()) issues += TimetableImportIssue(null, "EMPTY_SCOPE", "교체할 전철역을 선택해주세요.")
        if (request.entries.isEmpty()) issues += TimetableImportIssue(null, "EMPTY_IMPORT", "업로드할 시간표가 없습니다.")

        val allStationIDs =
            (scope + request.entries.flatMap { listOf(it.stationID, it.startStationID, it.terminalStationID) }).distinct()
        val existingStationIDs = stationRepository.findByIdIn(allStationIDs).map { it.id }.toSet()
        request.entries.forEachIndexed { index, entry ->
            val row = index + 1
            if (entry.stationID !in scope) issues += TimetableImportIssue(row, "OUT_OF_SCOPE", "교체 대상이 아닌 역입니다: ${entry.stationID}")
            if (runCatching { LocalTime.parse(entry.departureTime) }.isFailure) {
                issues += TimetableImportIssue(row, "INVALID_TIME", "출발 시각 형식이 올바르지 않습니다.")
            }
            if (entry.weekday !in WEEKDAYS) issues += TimetableImportIssue(row, "INVALID_WEEKDAY", "요일 구분이 올바르지 않습니다.")
            if (entry.direction !in DIRECTIONS) issues += TimetableImportIssue(row, "INVALID_DIRECTION", "행선 구분이 올바르지 않습니다.")
            listOf(entry.stationID, entry.startStationID, entry.terminalStationID)
                .filterNot(existingStationIDs::contains)
                .distinct()
                .forEach { stationID ->
                    issues += TimetableImportIssue(row, "STATION_NOT_FOUND", "존재하지 않는 역입니다: $stationID")
                }
        }
        request.entries
            .groupBy { entry -> runCatching { key(entry) }.getOrNull() }
            .filter { (timetableKey, entries) -> timetableKey != null && entries.size > 1 }
            .forEach { (_, entries) ->
                issues += TimetableImportIssue(null, "DUPLICATE_ENTRY", "중복된 시간표가 ${entries.size}건 있습니다: ${entries.first().description()}")
            }
        return issues
    }

    private fun emptyPreview(errors: List<TimetableImportIssue>) =
        TimetableImportPreviewResponse(null, null, 0, 0, 0, 0, errors, emptyList(), emptyList())

    private fun fingerprint(timetables: List<SubwayTimetable>) =
        TimetableImportSupport.fingerprint(timetables.map { "${key(it)}|${it.startStationID}|${it.terminalStationID}" })

    private fun key(timetable: SubwayTimetable) =
        "${timetable.stationID}|${timetable.weekday}|${timetable.heading}|${timetable.departureTime}"

    private fun key(timetable: BulkSubwayTimetableCreateRequest) =
        "${timetable.stationID}|${timetable.weekday}|${timetable.direction}|${LocalTime.parse(timetable.departureTime)}"

    private fun SubwayTimetable.description() = "$startStationID → $terminalStationID"

    private fun BulkSubwayTimetableCreateRequest.description() = "$startStationID → $terminalStationID"

    private fun change(
        type: String,
        identifier: String,
        before: String?,
        after: String?,
    ) = TimetableImportChange(type, identifier.replace("|", " · "), before, after)

    private fun MutableList<TimetableImportChange>.remainingCapacity() = (SAMPLE_LIMIT - size).coerceAtLeast(0)

    private fun toEntity(payload: BulkSubwayTimetableCreateRequest) =
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

    companion object {
        private const val TYPE = "subway"
        private const val SAMPLE_LIMIT = 20
        private val WEEKDAYS = setOf("weekdays", "weekends")
        private val DIRECTIONS = setOf("up", "down")
    }
}
