package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.service.BusRealtimeService
import app.hyuabot.backend.database.entity.BusRealtime
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture

@DgsDataLoader(name = "busRealtimeDataLoader")
class BusRealtimeDataLoader(
    private val realtimeService: BusRealtimeService,
) : MappedBatchLoader<Pair<Int, Int>, List<BusRealtime>> {
    override fun load(keys: Set<Pair<Int, Int>>): CompletableFuture<Map<Pair<Int, Int>, List<BusRealtime>>> =
        CompletableFuture.supplyAsync { realtimeService.getBusRealtimeBatch(keys) }
}
