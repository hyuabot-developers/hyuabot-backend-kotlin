package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.BusArrivalKey
import app.hyuabot.backend.bus.service.BusRealtimeService
import app.hyuabot.backend.codegen.types.BusArrival
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture

@DgsDataLoader(name = "busArrivalDataLoader")
class BusArrivalDataLoader(
    private val realtimeService: BusRealtimeService,
) : MappedBatchLoader<BusArrivalKey, List<BusArrival>> {
    override fun load(keys: Set<BusArrivalKey>): CompletableFuture<Map<BusArrivalKey, List<BusArrival>>> =
        CompletableFuture.supplyAsync { realtimeService.getArrivalBatch(keys) }
}
