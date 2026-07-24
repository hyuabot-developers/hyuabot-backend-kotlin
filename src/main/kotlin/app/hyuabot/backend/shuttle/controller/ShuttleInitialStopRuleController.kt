package app.hyuabot.backend.shuttle.controller

import app.hyuabot.backend.database.entity.ShuttleInitialStopRule
import app.hyuabot.backend.shuttle.domain.ShuttleInitialStopRuleListResponse
import app.hyuabot.backend.shuttle.domain.ShuttleInitialStopRuleRequest
import app.hyuabot.backend.shuttle.domain.ShuttleInitialStopRuleResponse
import app.hyuabot.backend.shuttle.exception.InvalidShuttleInitialStopRuleException
import app.hyuabot.backend.shuttle.exception.ShuttleInitialStopRuleNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleInitialStopRuleService
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/shuttle/initial-stop-rule")
@RestController
@Tag(name = "Shuttle", description = "셔틀버스 관련 API")
class ShuttleInitialStopRuleController(
    private val service: ShuttleInitialStopRuleService,
) {
    @GetMapping
    @Operation(summary = "초기 정류장 규칙 목록 조회")
    fun getAll(): ShuttleInitialStopRuleListResponse = ShuttleInitialStopRuleListResponse(service.getAll().map { it.toResponse() })

    @GetMapping("/{seq}")
    @Operation(summary = "초기 정류장 규칙 조회")
    fun get(
        @PathVariable seq: Int,
    ): ShuttleInitialStopRuleResponse = service.get(seq).toResponse()

    @PostMapping
    @Operation(summary = "초기 정류장 규칙 생성")
    fun create(
        @RequestBody request: ShuttleInitialStopRuleRequest,
    ): ResponseEntity<ShuttleInitialStopRuleResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request).toResponse())

    @PutMapping("/{seq}")
    @Operation(summary = "초기 정류장 규칙 수정")
    fun update(
        @PathVariable seq: Int,
        @RequestBody request: ShuttleInitialStopRuleRequest,
    ): ShuttleInitialStopRuleResponse = service.update(seq, request).toResponse()

    @DeleteMapping("/{seq}")
    @Operation(summary = "초기 정류장 규칙 삭제")
    fun delete(
        @PathVariable seq: Int,
    ): ResponseEntity<Void> {
        service.delete(seq)
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(ShuttleInitialStopRuleNotFoundException::class)
    fun handleNotFound(): ResponseEntity<ResponseBuilder.Message> =
        ResponseBuilder.response(
            HttpStatus.NOT_FOUND,
            ResponseBuilder.Message("SHUTTLE_INITIAL_STOP_RULE_NOT_FOUND"),
        )

    @ExceptionHandler(InvalidShuttleInitialStopRuleException::class)
    fun handleInvalid(): ResponseEntity<ResponseBuilder.Message> =
        ResponseBuilder.response(
            HttpStatus.BAD_REQUEST,
            ResponseBuilder.Message("INVALID_SHUTTLE_INITIAL_STOP_RULE"),
        )

    private fun ShuttleInitialStopRule.toResponse() =
        ShuttleInitialStopRuleResponse(
            seq = requireNotNull(seq),
            name = name,
            periodType = periodType,
            weekday = weekday,
            startTime = startTime,
            endTime = endTime,
            stopName = stopName,
            priority = priority,
            enabled = enabled,
            polygon = polygon,
        )
}
