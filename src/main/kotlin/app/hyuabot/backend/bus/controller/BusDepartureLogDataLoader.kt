package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.BusDepartureLogKey
import app.hyuabot.backend.bus.service.BusRouteService
import app.hyuabot.backend.database.entity.BusDepartureLog
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture

@DgsDataLoader(name = "busDepartureLogDataLoader")
class BusDepartureLogDataLoader(
    private val routeService: BusRouteService,
) : MappedBatchLoader<BusDepartureLogKey, List<BusDepartureLog>> {
    override fun load(keys: Set<BusDepartureLogKey>): CompletableFuture<Map<BusDepartureLogKey, List<BusDepartureLog>>> =
        CompletableFuture.completedFuture(routeService.getBusDepartureLogBatch(keys))
}
