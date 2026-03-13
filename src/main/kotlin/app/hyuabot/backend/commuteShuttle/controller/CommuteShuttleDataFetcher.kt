package app.hyuabot.backend.commuteShuttle.controller

import app.hyuabot.backend.codegen.types.CommuteShuttleDescription
import app.hyuabot.backend.codegen.types.CommuteShuttleRoute
import app.hyuabot.backend.codegen.types.CommuteShuttleTimetable
import app.hyuabot.backend.commuteShuttle.CommuteShuttleService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

@DgsComponent
class CommuteShuttleDataFetcher(
    private val commuteShuttleService: CommuteShuttleService,
) {
    @DgsQuery
    fun commute(): List<CommuteShuttleRoute> =
        commuteShuttleService.getAllRoutes().let { routes ->
            routes.map { route ->
                CommuteShuttleRoute(
                    name = route.name,
                    description =
                        CommuteShuttleDescription(
                            korean = route.descriptionKorean,
                            english = route.descriptionEnglish,
                        ),
                    timetable =
                        route.timetable
                            .sortedBy {
                                it.order
                            }.map { timetable ->
                                CommuteShuttleTimetable(
                                    order = timetable.order,
                                    name = timetable.stopName,
                                    time = timetable.departureTime,
                                )
                            },
                )
            }
        }
}
