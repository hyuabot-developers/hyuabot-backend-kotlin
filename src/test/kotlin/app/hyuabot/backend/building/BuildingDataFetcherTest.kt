package app.hyuabot.backend.building

import app.hyuabot.backend.database.entity.Building
import app.hyuabot.backend.database.entity.Room
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@EnableDgsTest
@SpringJUnitConfig(BuildingDataFetcher::class)
@Import(BuildingDataFetcher::class, ScalarRegistration::class)
class BuildingDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var buildingService: BuildingService

    private fun createBuilding(
        id: String = UUID.randomUUID().toString(),
        name: String = UUID.randomUUID().toString(),
        campusID: Int = 1,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        rooms: List<Room> = emptyList(),
    ): Building =
        Building(
            id = id,
            name = name,
            campusID = campusID,
            latitude = latitude,
            longitude = longitude,
            room = rooms,
        )

    private fun createRoom(
        seq: Int = 1,
        buildingName: String = UUID.randomUUID().toString(),
        number: String = "101",
        name: String = "Room 101",
    ): Room =
        Room(
            seq = seq,
            buildingName = buildingName,
            number = number,
            name = name,
        )

    @Test
    @DisplayName("건물 목록과 각 건물의 방 목록을 올바르게 반환하는지 테스트")
    fun testFetchBuilding() {
        whenever(buildingService.fetchBuildings(null)).thenReturn(
            listOf(
                createBuilding(
                    name = "Building A",
                    rooms =
                        listOf(
                            createRoom(
                                seq = 1,
                                buildingName = "Building A",
                                number = "103",
                                name = "Room 103",
                            ),
                            createRoom(
                                seq = 2,
                                buildingName = "Building A",
                                number = "102",
                                name = "Room 102",
                            ),
                        ),
                ),
                createBuilding(name = "Building B"),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    building {
                        name
                        rooms {
                            number
                            name
                        }
                    }
                }
                """.trimIndent(),
                "data.building",
            )
        assertEquals(2, result.size)
        assertEquals("Building A", result[0]["name"])
        assertEquals("Building B", result[1]["name"])
    }
}
