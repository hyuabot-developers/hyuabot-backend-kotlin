package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.MinimumDispatchInterval
import app.hyuabot.backend.bus.service.BusTimetableService
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture

@DgsDataLoader(name = "busMinimumDispatchIntervalDataLoader")
class BusMinimumDispatchIntervalDataLoader(
    private val timetableService: BusTimetableService,
) : MappedBatchLoader<Pair<Int, Int>, List<MinimumDispatchInterval>> {
    override fun load(keys: Set<Pair<Int, Int>>): CompletableFuture<Map<Pair<Int, Int>, List<MinimumDispatchInterval>>> =
        CompletableFuture.completedFuture(timetableService.getMinimumDispatchIntervalsBatch(keys))
}
