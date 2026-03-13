package app.hyuabot.backend.commuteShuttle.controller

import app.hyuabot.backend.codegen.types.CommuteShuttleDescription
import app.hyuabot.backend.codegen.types.CommuteShuttleRoute
import app.hyuabot.backend.codegen.types.CommuteShuttleStop
import app.hyuabot.backend.codegen.types.CommuteShuttleTimetable
import app.hyuabot.backend.commuteShuttle.CommuteShuttleService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import org.springframework.transaction.annotation.Transactional

@DgsComponent
class CommuteShuttleDataFetcher(
    private val commuteShuttleService: CommuteShuttleService,
) {
    @DgsQuery
    @Transactional
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
                                    stop =
                                        timetable.stop?.let { stop ->
                                            CommuteShuttleStop(
                                                name = stop.name,
                                                latitude = stop.latitude,
                                                longitude = stop.longitude,
                                                description = stop.description,
                                            )
                                        },
                                )
                            },
                )
            }
        }
}
