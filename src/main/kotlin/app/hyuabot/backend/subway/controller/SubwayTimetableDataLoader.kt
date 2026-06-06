package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.codegen.types.SubwayTimetable
import app.hyuabot.backend.subway.domain.SubwayTimetableKey
import app.hyuabot.backend.subway.service.SubwayService
import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture

@DgsDataLoader(name = "subwayTimetableDataLoader")
class SubwayTimetableDataLoader(
    private val subwayService: SubwayService,
) : MappedBatchLoader<SubwayTimetableKey, List<SubwayTimetable>> {
    override fun load(keys: Set<SubwayTimetableKey>): CompletableFuture<Map<SubwayTimetableKey, List<SubwayTimetable>>> =
        CompletableFuture.completedFuture(
            keys.associateWith {
                subwayService.getTimetableView(it.stationID, it.directions.sorted(), it.weekdays.sorted())
            },
        )
}
