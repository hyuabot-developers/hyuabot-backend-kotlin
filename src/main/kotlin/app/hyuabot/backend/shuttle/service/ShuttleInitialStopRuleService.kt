package app.hyuabot.backend.shuttle.service

import app.hyuabot.backend.database.entity.ShuttleInitialStopRule
import app.hyuabot.backend.database.repository.ShuttleInitialStopRuleRepository
import app.hyuabot.backend.database.repository.ShuttlePeriodTypeRepository
import app.hyuabot.backend.database.repository.ShuttleStopRepository
import app.hyuabot.backend.shuttle.domain.ShuttleGeoPoint
import app.hyuabot.backend.shuttle.domain.ShuttleInitialStopRuleRequest
import app.hyuabot.backend.shuttle.exception.InvalidShuttleInitialStopRuleException
import app.hyuabot.backend.shuttle.exception.ShuttleInitialStopRuleNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import kotlin.math.abs

@Service
class ShuttleInitialStopRuleService(
    private val repository: ShuttleInitialStopRuleRepository,
    private val periodTypeRepository: ShuttlePeriodTypeRepository,
    private val stopRepository: ShuttleStopRepository,
) {
    fun getAll(): List<ShuttleInitialStopRule> = repository.findAllByOrderByPriorityDescSeqAsc()

    fun get(seq: Int): ShuttleInitialStopRule = repository.findById(seq).orElseThrow { ShuttleInitialStopRuleNotFoundException() }

    @Transactional
    fun create(request: ShuttleInitialStopRuleRequest): ShuttleInitialStopRule {
        validate(request)
        return repository.save(
            ShuttleInitialStopRule(
                name = request.name.trim(),
                periodType = request.periodType,
                weekday = request.weekday,
                startTime = request.startTime,
                endTime = request.endTime,
                stopName = request.stopName,
                priority = request.priority,
                enabled = request.enabled,
                polygon = request.polygon,
            ),
        )
    }

    @Transactional
    fun update(
        seq: Int,
        request: ShuttleInitialStopRuleRequest,
    ): ShuttleInitialStopRule {
        validate(request)
        val rule = get(seq)
        rule.name = request.name.trim()
        rule.periodType = request.periodType
        rule.weekday = request.weekday
        rule.startTime = request.startTime
        rule.endTime = request.endTime
        rule.stopName = request.stopName
        rule.priority = request.priority
        rule.enabled = request.enabled
        rule.polygon = request.polygon
        return repository.save(rule)
    }

    @Transactional
    fun delete(seq: Int) {
        repository.delete(get(seq))
    }

    fun getActive(
        periodTypes: Collection<String>,
        weekdays: Collection<Boolean>,
        time: LocalTime,
        isHalt: Boolean,
    ): List<ShuttleInitialStopRule> {
        if (isHalt || periodTypes.isEmpty() || weekdays.isEmpty()) return emptyList()
        return repository
            .findByPeriodTypeInAndWeekdayInAndEnabledTrueOrderByPriorityDescSeqAsc(periodTypes, weekdays)
            .filter { isActiveAt(it, time) }
    }

    internal fun isActiveAt(
        rule: ShuttleInitialStopRule,
        time: LocalTime,
    ): Boolean {
        val start = rule.startTime ?: return true
        val end = rule.endTime ?: return true
        return if (start < end) {
            time >= start && time < end
        } else {
            time >= start || time < end
        }
    }

    private fun validate(request: ShuttleInitialStopRuleRequest) {
        if (request.name.isBlank() || request.name.trim().length > 80) {
            throw InvalidShuttleInitialStopRuleException("Rule name must contain 1 to 80 characters")
        }
        if (!periodTypeRepository.existsById(request.periodType)) {
            throw InvalidShuttleInitialStopRuleException("Unknown shuttle period type")
        }
        if (!stopRepository.existsById(request.stopName)) {
            throw InvalidShuttleInitialStopRuleException("Unknown shuttle stop")
        }
        if (
            (request.startTime == null) != (request.endTime == null) ||
            (request.startTime != null && request.startTime == request.endTime)
        ) {
            throw InvalidShuttleInitialStopRuleException("Start and end time must both be empty or distinct")
        }
        validatePolygon(request.polygon)
    }

    private fun validatePolygon(polygon: List<ShuttleGeoPoint>) {
        if (polygon.size !in 3..MAX_POLYGON_VERTICES) {
            throw InvalidShuttleInitialStopRuleException("Polygon must contain 3 to $MAX_POLYGON_VERTICES vertices")
        }
        if (polygon.any { it.latitude !in -90.0..90.0 || it.longitude !in -180.0..180.0 }) {
            throw InvalidShuttleInitialStopRuleException("Polygon contains an invalid coordinate")
        }
        if (polygon.distinct().size < 3 || abs(signedArea(polygon)) < MIN_POLYGON_AREA) {
            throw InvalidShuttleInitialStopRuleException("Polygon must have a non-zero area")
        }
        if (hasSelfIntersection(polygon)) {
            throw InvalidShuttleInitialStopRuleException("Polygon must not intersect itself")
        }
    }

    private fun signedArea(polygon: List<ShuttleGeoPoint>): Double =
        polygon.indices.sumOf { index ->
            val current = polygon[index]
            val next = polygon[(index + 1) % polygon.size]
            current.longitude * next.latitude - next.longitude * current.latitude
        } / 2.0

    private fun hasSelfIntersection(polygon: List<ShuttleGeoPoint>): Boolean =
        polygon.indices.any { first ->
            polygon.indices.any { second ->
                if (first == second || (first + 1) % polygon.size == second || first == (second + 1) % polygon.size) {
                    false
                } else {
                    segmentsIntersect(
                        polygon[first],
                        polygon[(first + 1) % polygon.size],
                        polygon[second],
                        polygon[(second + 1) % polygon.size],
                    )
                }
            }
        }

    private fun segmentsIntersect(
        a: ShuttleGeoPoint,
        b: ShuttleGeoPoint,
        c: ShuttleGeoPoint,
        d: ShuttleGeoPoint,
    ): Boolean {
        val abC = cross(a, b, c)
        val abD = cross(a, b, d)
        val cdA = cross(c, d, a)
        val cdB = cross(c, d, b)
        return abC * abD < 0 && cdA * cdB < 0
    }

    private fun cross(
        a: ShuttleGeoPoint,
        b: ShuttleGeoPoint,
        c: ShuttleGeoPoint,
    ): Double =
        (b.longitude - a.longitude) * (c.latitude - a.latitude) -
            (b.latitude - a.latitude) * (c.longitude - a.longitude)

    companion object {
        private const val MAX_POLYGON_VERTICES = 100
        private const val MIN_POLYGON_AREA = 1e-12
    }
}
