package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttlePeriod
import app.hyuabot.backend.database.repository.ShuttlePeriodRepository
import app.hyuabot.backend.shuttle.domain.ShuttlePeriodRequest
import app.hyuabot.backend.shuttle.exception.ShuttlePeriodNotFoundException
import org.springframework.stereotype.Service

@Service
class ShuttlePeriodService(
    private val shuttlePeriodRepository: ShuttlePeriodRepository,
) {
    fun getShuttlePeriodList() = shuttlePeriodRepository.findAll().sortedBy { it.start }

    fun createShuttlePeriod(payload: ShuttlePeriodRequest) {
        shuttlePeriodRepository.save(
            ShuttlePeriod(
                start = payload.start,
                end = payload.end,
                type = payload.type,
                periodType = null,
            ),
        )
    }

    fun getShuttlePeriod(seq: Int): ShuttlePeriod =
        shuttlePeriodRepository.findById(seq).orElseThrow {
            throw ShuttlePeriodNotFoundException()
        }

    fun updateShuttlePeriod(
        seq: Int,
        payload: ShuttlePeriodRequest,
    ): ShuttlePeriod =
        shuttlePeriodRepository
            .findById(seq)
            .orElseThrow {
                throw ShuttlePeriodNotFoundException()
            }.let {
                it.apply {
                    start = payload.start
                    end = payload.end
                    type = payload.type
                }
                shuttlePeriodRepository.save(it)
            }

    fun deleteShuttlePeriod(seq: Int) {
        shuttlePeriodRepository
            .findById(seq)
            .orElseThrow {
                throw ShuttlePeriodNotFoundException()
            }.let {
                shuttlePeriodRepository.delete(it)
            }
    }
}
