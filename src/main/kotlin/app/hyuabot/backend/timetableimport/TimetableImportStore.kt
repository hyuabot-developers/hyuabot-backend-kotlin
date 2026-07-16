package app.hyuabot.backend.timetableimport

import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

data class StoredTimetableImport(
    val id: String,
    val expiresAt: ZonedDateTime,
)

@Component
class TimetableImportStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @param:Value("\${admin.timetable-import.preview-ttl-minutes:15}") private val ttlMinutes: Long,
) {
    fun save(
        type: String,
        snapshot: Any,
    ): StoredTimetableImport {
        val id = UUID.randomUUID().toString()
        val ttl = Duration.ofMinutes(ttlMinutes)
        redisTemplate.opsForValue().set(dataKey(type, id), objectMapper.writeValueAsString(snapshot), ttl)
        return StoredTimetableImport(
            id = id,
            expiresAt = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone).plus(ttl),
        )
    }

    fun <T> load(
        type: String,
        id: String,
        valueType: Class<T>,
    ): T {
        val value = redisTemplate.opsForValue().get(dataKey(type, id)) ?: throw TimetableImportException("PREVIEW_NOT_FOUND")
        return objectMapper.readValue(value, valueType)
    }

    fun acquire(
        type: String,
        id: String,
    ) {
        val acquired = redisTemplate.opsForValue().setIfAbsent(lockKey(type, id), "locked", Duration.ofMinutes(1)) == true
        if (!acquired) throw TimetableImportException("PREVIEW_ALREADY_APPLYING")
    }

    fun consume(
        type: String,
        id: String,
    ) {
        redisTemplate.delete(dataKey(type, id))
    }

    fun release(
        type: String,
        id: String,
    ) {
        redisTemplate.delete(lockKey(type, id))
    }

    private fun dataKey(
        type: String,
        id: String,
    ) = "admin:timetable-import:$type:$id"

    private fun lockKey(
        type: String,
        id: String,
    ) = "admin:timetable-import:$type:$id:lock"
}
