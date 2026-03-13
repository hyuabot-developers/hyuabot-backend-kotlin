package app.hyuabot.backend.commuteShuttle

import app.hyuabot.backend.commuteShuttle.controller.CommuteShuttleDataFetcher
import app.hyuabot.backend.database.entity.CommuteShuttleRoute
import app.hyuabot.backend.database.entity.CommuteShuttleTimetable
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.assertEquals

@EnableDgsTest
@SpringJUnitConfig
@Import(CommuteShuttleDataFetcher::class, ScalarRegistration::class)
class CommuteShuttleDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var commuteShuttleService: CommuteShuttleService

    private val now = ZonedDateTime.now()

    private fun createCommuteShuttleRoute(
        name: String = "Route 1",
        descriptionKorean: String = "통학버스 노선 1",
        descriptionEnglish: String = "Commute Shuttle Route 1",
        timetable: List<CommuteShuttleTimetable> = listOf(),
    ) = CommuteShuttleRoute(
        name = name,
        descriptionKorean = descriptionKorean,
        descriptionEnglish = descriptionEnglish,
        timetable = timetable,
    )

    private fun createCommuteShuttleTimetable(
        seq: Int = 1,
        order: Int = 1,
        routeName: String,
        stopName: String = "Stop 1",
        departureTime: LocalTime = now.toLocalTime(),
    ) = CommuteShuttleTimetable(
        seq = seq,
        order = order,
        routeName = routeName,
        stopName = stopName,
        departureTime = departureTime,
        route = null,
        stop = null,
    )

    @Test
    @DisplayName("통학버스 노선 및 시간표 조회 테스트")
    fun testCommuteShuttle() {
        whenever(commuteShuttleService.getAllRoutes()).thenReturn(
            listOf(
                createCommuteShuttleRoute(
                    name = "Route 1",
                    descriptionKorean = "통학버스 노선 1",
                    descriptionEnglish = "Commute Shuttle Route 1",
                    timetable =
                        listOf(
                            createCommuteShuttleTimetable(
                                seq = 1,
                                order = 1,
                                routeName = "Route 1",
                                stopName = "Stop 1",
                                departureTime = now.toLocalTime(),
                            ),
                            createCommuteShuttleTimetable(
                                seq = 2,
                                order = 2,
                                routeName = "Route 1",
                                stopName = "Stop 2",
                                departureTime = now.toLocalTime() + Duration.ofMinutes(30),
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    commute {
                        name
                        description {
                            korean
                            english
                        }
                        timetable {
                            order
                            name
                            time
                        }
                    }
                }
                """.trimIndent(),
                "data.commute",
            )
        assertEquals(1, result.size)
        val route = result[0]
        assertEquals("Route 1", route["name"])
        val description = route["description"] as Map<*, *>
        assertEquals("통학버스 노선 1", description["korean"])
        assertEquals("Commute Shuttle Route 1", description["english"])
        val timetable = route["timetable"] as List<*>
        assertEquals(2, timetable.size)
        val firstStop = timetable[0] as Map<*, *>
        assertEquals(1, firstStop["order"])
        assertEquals("Stop 1", firstStop["name"])
        assertEquals(now.toLocalTime().toString(), firstStop["time"])
        val secondStop = timetable[1] as Map<*, *>
        assertEquals(2, secondStop["order"])
        assertEquals("Stop 2", secondStop["name"])
        assertEquals((now.toLocalTime() + Duration.ofMinutes(30)).toString(), secondStop["time"])
    }
}
