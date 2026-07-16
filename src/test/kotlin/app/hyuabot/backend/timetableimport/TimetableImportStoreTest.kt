package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportRequest
import app.hyuabot.backend.timetableimport.domain.ShuttleTimetableImportSnapshot
import app.hyuabot.backend.timetableimport.domain.TimetableImportApplyResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.databind.ObjectMapper
import java.time.Duration

class TimetableImportStoreTest {
    private val redisTemplate = mock<RedisTemplate<String, String>>()
    private val valueOperations = mock<ValueOperations<String, String>>()
    private val objectMapper = mock<ObjectMapper>()
    private val store = TimetableImportStore(redisTemplate, objectMapper, 15)
    private val snapshot =
        ShuttleTimetableImportSnapshot(
            ShuttleTimetableImportRequest(emptyList(), emptyList()),
            "fingerprint",
            TimetableImportApplyResponse(1, 0, 2, 3),
        )

    init {
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
    }

    @Test
    fun `save writes a TTL preview and load restores it`() {
        whenever(objectMapper.writeValueAsString(snapshot)).thenReturn("snapshot-json")
        val stored = store.save("shuttle", snapshot)
        verify(valueOperations).set(
            "admin:timetable-import:shuttle:${stored.id}",
            "snapshot-json",
            Duration.ofMinutes(15),
        )

        whenever(valueOperations.get("admin:timetable-import:shuttle:${stored.id}")).thenReturn("snapshot-json")
        whenever(objectMapper.readValue("snapshot-json", ShuttleTimetableImportSnapshot::class.java)).thenReturn(snapshot)
        assertEquals(snapshot, store.load("shuttle", stored.id, ShuttleTimetableImportSnapshot::class.java))
    }

    @Test
    fun `missing and busy previews are rejected while locks can be released`() {
        whenever(valueOperations.get(any())).thenReturn(null)
        assertEquals(
            "PREVIEW_NOT_FOUND",
            assertThrows(TimetableImportException::class.java) {
                store.load("subway", "missing", ShuttleTimetableImportSnapshot::class.java)
            }.code,
        )

        whenever(valueOperations.setIfAbsent(any(), any(), any<Duration>())).thenReturn(false)
        assertEquals(
            "PREVIEW_ALREADY_APPLYING",
            assertThrows(TimetableImportException::class.java) { store.acquire("subway", "busy") }.code,
        )

        whenever(valueOperations.setIfAbsent(any(), any(), any<Duration>())).thenReturn(true)
        store.acquire("subway", "available")
        store.consume("subway", "available")
        store.release("subway", "available")
        verify(redisTemplate).delete("admin:timetable-import:subway:available")
        verify(redisTemplate).delete("admin:timetable-import:subway:available:lock")
    }

    @Test
    fun `fingerprints are stable regardless of input ordering`() {
        assertEquals(
            TimetableImportSupport.fingerprint(listOf("a", "b")),
            TimetableImportSupport.fingerprint(listOf("b", "a")),
        )
    }
}
