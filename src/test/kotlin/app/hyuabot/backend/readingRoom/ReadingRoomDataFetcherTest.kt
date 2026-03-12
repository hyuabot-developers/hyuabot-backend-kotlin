package app.hyuabot.backend.readingRoom

import app.hyuabot.backend.database.entity.ReadingRoom
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
import java.time.ZonedDateTime
import kotlin.test.assertEquals

@EnableDgsTest
@SpringJUnitConfig
@Import(ReadingRoomDataFetcher::class, ScalarRegistration::class)
class ReadingRoomDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var readingRoomService: ReadingRoomService

    private val now = ZonedDateTime.now()

    private fun createReadingRoom(
        id: Int = 1,
        name: String = "Test Reading Room",
        campusID: Int = 1,
        isActive: Boolean = true,
        isReservable: Boolean = true,
        total: Int = 100,
        active: Int = 100,
        occupied: Int = 0,
        available: Int = 100,
    ) = ReadingRoom(
        id = id,
        name = name,
        campusID = campusID,
        isActive = isActive,
        isReservable = isReservable,
        total = total,
        active = active,
        occupied = occupied,
        available = available,
        updatedAt = now,
        campus = null,
    )

    @Test
    @DisplayName("열람실 데이터를 올바르게 반환하는지 테스트")
    fun testFetchReadingRoom() {
        whenever(readingRoomService.getReadingRoomList(null)).thenReturn(
            listOf(
                createReadingRoom(),
                createReadingRoom(
                    id = 2,
                    name = "Second Reading Room",
                    campusID = 1,
                    isActive = true,
                    isReservable = true,
                    total = 50,
                    active = 50,
                    occupied = 25,
                    available = 25,
                ),
                createReadingRoom(
                    id = 3,
                    name = "Third Reading Room",
                    campusID = 1,
                    isActive = true,
                    isReservable = true,
                    total = 50,
                    active = 50,
                    occupied = 50,
                    available = 50,
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    readingRoom {
                        seq
                        name
                        campus
                        active
                        reservable
                        seats {
                            total
                            active
                            occupied
                            available
                        }
                        updatedAt
                    }
                }
                """.trimIndent(),
                "data.readingRoom",
            )
        assertEquals(3, result.size)
        assertEquals(1, result[0]["seq"])
        assertEquals("Test Reading Room", result[0]["name"])
        assertEquals(1, result[0]["campus"])
        assertEquals(true, result[0]["active"])
        assertEquals(true, result[0]["reservable"])
        val seats1 = result[0]["seats"] as Map<*, *>
        assertEquals(100, seats1["total"])
        assertEquals(100, seats1["active"])
        assertEquals(0, seats1["occupied"])
        assertEquals(100, seats1["available"])
        assertEquals(2, result[1]["seq"])
        assertEquals("Second Reading Room", result[1]["name"])
        assertEquals(1, result[1]["campus"])
        assertEquals(true, result[1]["active"])
        assertEquals(true, result[1]["reservable"])
        val seats2 = result[1]["seats"] as Map<*, *>
        assertEquals(50, seats2["total"])
        assertEquals(50, seats2["active"])
        assertEquals(25, seats2["occupied"])
        assertEquals(25, seats2["available"])
        assertEquals(3, result[2]["seq"])
        assertEquals("Third Reading Room", result[2]["name"])
        assertEquals(1, result[2]["campus"])
        assertEquals(true, result[2]["active"])
        assertEquals(true, result[2]["reservable"])
        val seats3 = result[2]["seats"] as Map<*, *>
        assertEquals(50, seats3["total"])
        assertEquals(50, seats3["active"])
        assertEquals(50, seats3["occupied"])
        assertEquals(50, seats3["available"])
    }
}
