package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.database.entity.ShuttleTimetable
import app.hyuabot.backend.database.repository.ShuttlePeriodTypeRepository
import app.hyuabot.backend.database.repository.ShuttleRouteRepository
import app.hyuabot.backend.database.repository.ShuttleTimetableRepository
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportEntry
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportSnapshot
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyResponse
import app.hyuabot.backend.timetableimport.domain.TimetableImportChange
import app.hyuabot.backend.timetableimport.domain.TimetableImportIssue
import app.hyuabot.backend.timetableimport.domain.TimetableImportPreviewResponse
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
class ShuttleTimetableImportService(
    private val routeRepository: ShuttleRouteRepository,
    private val periodTypeRepository: ShuttlePeriodTypeRepository,
    private val timetableRepository: ShuttleTimetableRepository,
    private val store: TimetableImportStore,
) {
    fun preview(request: ShuttleTimetableImportRequest): TimetableImportPreviewResponse {
        val scope = request.routeNames.distinct()
        val errors = validate(request, scope)
        if (errors.isNotEmpty()) return emptyPreview(errors)

        val current = timetableRepository.findByRouteNameIn(scope)
        val currentKeys = current.map(::key).toSet()
        val incomingKeys = request.entries.map(::key).toSet()
        val createKeys = incomingKeys - currentKeys
        val deleteKeys = currentKeys - incomingKeys
        val unchangedCount = incomingKeys.intersect(currentKeys).size
        val result = TimetableImportApplyResponse(createKeys.size, 0, deleteKeys.size, unchangedCount)
        val snapshot =
            ShuttleTimetableImportSnapshot(
                request = request.copy(routeNames = scope),
                fingerprint = fingerprint(current),
                result = result,
            )
        val stored = store.save(TYPE, snapshot)
        val changes =
            buildList {
                createKeys.take(SAMPLE_LIMIT).forEach { timetableKey ->
                    add(TimetableImportChange("CREATE", timetableKey.replace("|", " · "), null, "새 시간표"))
                }
                deleteKeys.take((SAMPLE_LIMIT - size).coerceAtLeast(0)).forEach { timetableKey ->
                    add(TimetableImportChange("DELETE", timetableKey.replace("|", " · "), "기존 시간표", null))
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

    @CacheEvict(cacheNames = ["shuttleTimetable"], allEntries = true)
    @Transactional
    fun apply(previewID: String): TimetableImportApplyResponse {
        store.acquire(TYPE, previewID)
        try {
            val snapshot = store.load(TYPE, previewID, ShuttleTimetableImportSnapshot::class.java)
            val current = timetableRepository.findByRouteNameInForUpdate(snapshot.request.routeNames)
            if (fingerprint(current) != snapshot.fingerprint) throw TimetableImportException("PREVIEW_STALE")
            timetableRepository.deleteAllByRouteNameIn(snapshot.request.routeNames)
            timetableRepository.flush()
            timetableRepository.saveAll(snapshot.request.entries.map(::toEntity))
            store.consume(TYPE, previewID)
            return snapshot.result
        } finally {
            store.release(TYPE, previewID)
        }
    }

    private fun validate(
        request: ShuttleTimetableImportRequest,
        scope: List<String>,
    ): List<TimetableImportIssue> {
        val issues = mutableListOf<TimetableImportIssue>()
        if (scope.isEmpty()) issues += TimetableImportIssue(null, "EMPTY_SCOPE", "교체할 셔틀 노선을 선택해주세요.")
        if (request.entries.isEmpty()) issues += TimetableImportIssue(null, "EMPTY_IMPORT", "업로드할 시간표가 없습니다.")
        val existingRoutes = routeRepository.findAllById((scope + request.entries.map { it.routeName }).distinct()).map { it.name }.toSet()
        val existingPeriods = periodTypeRepository.findAllById(request.entries.map { it.period }.distinct()).map { it.type }.toSet()
        request.entries.forEachIndexed { index, entry ->
            val row = index + 1
            if (entry.routeName !in scope) issues += TimetableImportIssue(row, "OUT_OF_SCOPE", "교체 대상이 아닌 노선입니다: ${entry.routeName}")
            if (entry.routeName !in
                existingRoutes
            ) {
                issues += TimetableImportIssue(row, "ROUTE_NOT_FOUND", "존재하지 않는 노선입니다: ${entry.routeName}")
            }
            if (entry.period !in
                existingPeriods
            ) {
                issues += TimetableImportIssue(row, "PERIOD_NOT_FOUND", "존재하지 않는 운행 종류입니다: ${entry.period}")
            }
            if (runCatching { LocalTime.parse(entry.departureTime) }.isFailure) {
                issues += TimetableImportIssue(row, "INVALID_TIME", "출발 시각 형식이 올바르지 않습니다.")
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

    private fun fingerprint(timetables: List<ShuttleTimetable>) = TimetableImportSupport.fingerprint(timetables.map(::key))

    private fun key(timetable: ShuttleTimetable) =
        "${timetable.routeName}|${timetable.periodType}|${timetable.weekday}|${timetable.departureTime}"

    private fun key(timetable: ShuttleTimetableImportEntry) =
        "${timetable.routeName}|${timetable.period}|${timetable.weekday}|${LocalTime.parse(timetable.departureTime)}"

    private fun ShuttleTimetableImportEntry.description() = "$routeName · $period · ${if (weekday) "평일" else "주말"} · $departureTime"

    private fun toEntity(payload: ShuttleTimetableImportEntry) =
        ShuttleTimetable(
            routeName = payload.routeName,
            periodType = payload.period,
            weekday = payload.weekday,
            departureTime = LocalTime.parse(payload.departureTime),
            route = null,
        )

    companion object {
        private const val TYPE = "shuttle"
        private const val SAMPLE_LIMIT = 20
    }
}
