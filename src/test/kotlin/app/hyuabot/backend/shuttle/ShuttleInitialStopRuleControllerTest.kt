package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleInitialStopRule
import app.hyuabot.backend.shuttle.controller.ShuttleInitialStopRuleController
import app.hyuabot.backend.shuttle.domain.ShuttleGeoPoint
import app.hyuabot.backend.shuttle.domain.ShuttleInitialStopRuleRequest
import app.hyuabot.backend.shuttle.service.ShuttleInitialStopRuleService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.time.LocalTime
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ShuttleInitialStopRuleControllerTest {
    @Mock lateinit var service: ShuttleInitialStopRuleService

    private val polygon =
        listOf(
            ShuttleGeoPoint(37.29, 126.83),
            ShuttleGeoPoint(37.30, 126.83),
            ShuttleGeoPoint(37.30, 126.84),
        )

    private val request =
        ShuttleInitialStopRuleRequest(
            name = "Campus morning",
            periodType = "semester",
            weekday = true,
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(10, 0),
            stopName = "station",
            priority = 100,
            enabled = true,
            polygon = polygon,
        )

    private val rule =
        ShuttleInitialStopRule(
            seq = 1,
            name = request.name,
            periodType = request.periodType,
            weekday = request.weekday,
            startTime = request.startTime,
            endTime = request.endTime,
            stopName = request.stopName,
            priority = request.priority,
            enabled = request.enabled,
            polygon = request.polygon,
        )

    @Test
    fun crudAndExceptionHandlers() {
        val controller = ShuttleInitialStopRuleController(service)
        whenever(service.getAll()).thenReturn(listOf(rule))
        whenever(service.get(1)).thenReturn(rule)
        whenever(service.create(request)).thenReturn(rule)
        whenever(service.update(1, request)).thenReturn(rule)
        doNothing().whenever(service).delete(1)

        assertEquals(
            "Campus morning",
            controller
                .getAll()
                .result
                .single()
                .name,
        )
        val response = controller.get(1)
        assertEquals(1, response.seq)
        assertEquals("semester", response.periodType)
        assertEquals(true, response.weekday)
        assertEquals(LocalTime.of(7, 0), response.startTime)
        assertEquals(LocalTime.of(10, 0), response.endTime)
        assertEquals("station", response.stopName)
        assertEquals(true, response.enabled)
        assertEquals(polygon, response.polygon)
        assertEquals(HttpStatus.CREATED, controller.create(request).statusCode)
        assertEquals(100, controller.update(1, request).priority)
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(1).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.handleNotFound().statusCode)
        assertEquals(
            "SHUTTLE_INITIAL_STOP_RULE_NOT_FOUND",
            controller.handleNotFound().body?.message,
        )
        assertEquals(HttpStatus.BAD_REQUEST, controller.handleInvalid().statusCode)
        assertEquals(
            "INVALID_SHUTTLE_INITIAL_STOP_RULE",
            controller.handleInvalid().body?.message,
        )

        whenever(service.get(2)).thenReturn(
            ShuttleInitialStopRule(
                seq = null,
                name = rule.name,
                periodType = rule.periodType,
                weekday = rule.weekday,
                startTime = rule.startTime,
                endTime = rule.endTime,
                stopName = rule.stopName,
                priority = rule.priority,
                enabled = rule.enabled,
                polygon = rule.polygon,
            ),
        )
        assertThrows<IllegalArgumentException> { controller.get(2) }
    }
}
