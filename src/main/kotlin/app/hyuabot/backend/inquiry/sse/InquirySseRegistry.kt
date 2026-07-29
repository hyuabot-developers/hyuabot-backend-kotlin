package app.hyuabot.backend.inquiry.sse

import app.hyuabot.backend.inquiry.domain.InquiryEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
open class InquirySseRegistry(
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    val installationEmitters = ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>()
    val adminEmitters = CopyOnWriteArrayList<SseEmitter>()

    internal open fun createEmitter(timeoutMs: Long): SseEmitter = SseEmitter(timeoutMs)

    fun registerForInstallation(
        installationId: String,
        timeoutMs: Long = DEFAULT_TIMEOUT,
    ): SseEmitter {
        val emitter = createEmitter(timeoutMs)
        val emitters = installationEmitters.computeIfAbsent(installationId) { CopyOnWriteArrayList() }
        emitters.add(emitter)
        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
        emitter.onError { emitters.remove(emitter) }
        sendInit(emitter)
        return emitter
    }

    fun registerForAdmin(timeoutMs: Long = DEFAULT_TIMEOUT): SseEmitter {
        val emitter = createEmitter(timeoutMs)
        adminEmitters.add(emitter)
        emitter.onCompletion { adminEmitters.remove(emitter) }
        emitter.onTimeout { adminEmitters.remove(emitter) }
        emitter.onError { adminEmitters.remove(emitter) }
        sendInit(emitter)
        return emitter
    }

    fun dispatch(event: InquiryEvent) {
        val json = objectMapper.writeValueAsString(event)
        installationEmitters[event.installationId]?.let { emitters ->
            emitters.forEach { sendTo(emitters, it, event.kind, json) }
        }
        adminEmitters.forEach { sendTo(adminEmitters, it, event.kind, json) }
    }

    private fun sendInit(emitter: SseEmitter) {
        try {
            emitter.send(SseEmitter.event().name("init").data("connected"))
        } catch (e: Exception) {
            logger.warn("Failed to send init SSE event", e)
        }
    }

    private fun sendTo(
        emitters: CopyOnWriteArrayList<SseEmitter>,
        emitter: SseEmitter,
        name: String,
        json: String,
    ) {
        try {
            emitter.send(SseEmitter.event().name(name).data(json))
        } catch (e: Exception) {
            emitters.remove(emitter)
            emitter.completeWithError(e)
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT = 1800_000L
    }
}
