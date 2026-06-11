package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.BusTimetableKey
import app.hyuabot.backend.bus.service.BusTimetableService
import app.hyuabot.backend.database.entity.BusTimetable
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture

@DgsDataLoader(name = "busTimetableDataLoader")
class BusTimetableDataLoader(
    private val timetableService: BusTimetableService,
) : MappedBatchLoader<BusTimetableKey, List<BusTimetable>> {
    override fun load(keys: Set<BusTimetableKey>): CompletableFuture<Map<BusTimetableKey, List<BusTimetable>>> =
        CompletableFuture.completedFuture(timetableService.getBusTimetableBatch(keys))
}
