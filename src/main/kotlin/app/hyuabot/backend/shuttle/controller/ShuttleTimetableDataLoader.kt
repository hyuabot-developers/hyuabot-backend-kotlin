package app.hyuabot.backend.shuttle.controller

import app.hyuabot.backend.shuttle.domain.ShuttleTimetableKey
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableResult
import app.hyuabot.backend.shuttle.service.ShuttleTimetableService
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

@DgsDataLoader(name = "shuttleTimetableDataLoader")
class ShuttleTimetableDataLoader(
    private val timetableService: ShuttleTimetableService,
) : MappedBatchLoader<ShuttleTimetableKey, ShuttleTimetableResult> {
    override fun load(keys: Set<ShuttleTimetableKey>): CompletionStage<Map<ShuttleTimetableKey, ShuttleTimetableResult>> =
        CompletableFuture.completedFuture(timetableService.getShuttleTimetableBatch(keys))
}
