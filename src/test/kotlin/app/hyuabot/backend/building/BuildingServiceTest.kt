package app.hyuabot.backend.building

import app.hyuabot.backend.building.domain.CreateBuildingRequest
import app.hyuabot.backend.building.domain.UpdateBuildingRequest
import app.hyuabot.backend.building.exception.BuildingNotFoundException
import app.hyuabot.backend.building.exception.DuplicateBuildingNameException
import app.hyuabot.backend.database.entity.Building
import app.hyuabot.backend.database.repository.BuildingRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class BuildingServiceTest {
    @Mock
    lateinit var buildingRepository: BuildingRepository

    @InjectMocks
    lateinit var buildingService: BuildingService

    @Test
    @DisplayName("건물 목록 조회 테스트")
    fun testGetBuildingList() {
        whenever(buildingRepository.findAll()).thenReturn(
            listOf(
                Building(
                    id = "Y001",
                    name = "Building A",
                    campusID = 1,
                    latitude = 37.5665,
                    longitude = 126.978,
                    url = "http://example.com/building-a",
                ),
                Building(
                    id = "Y002",
                    name = "Building B",
                    campusID = 2,
                    latitude = 37.5665,
                    longitude = 126.978,
                ),
            ),
        )
        val buildings = buildingService.getBuildingList()
        assertEquals(2, buildings.size)
        assertEquals("Building A", buildings[0].name)
        assertEquals("Building B", buildings[1].name)
        assertEquals(1, buildings[0].campusID)
        assertEquals(2, buildings[1].campusID)
        assertEquals(37.5665, buildings[0].latitude)
        assertEquals(126.978, buildings[0].longitude)
        assertEquals(37.5665, buildings[1].latitude)
        assertEquals(126.978, buildings[1].longitude)
        assertEquals("http://example.com/building-a", buildings[0].url)
        assertEquals(null, buildings[1].url)
    }

    @Test
    @DisplayName("건물 목록 조회 테스트 (캠퍼스 ID 필터링)")
    fun testGetBuildingListWithCampusID() {
        whenever(buildingRepository.findByCampusID(1)).thenReturn(
            listOf(
                Building(
                    id = "Y001",
                    name = "Building A",
                    campusID = 1,
                    latitude = 37.5665,
                    longitude = 126.978,
                    url = "http://example.com/building-a",
                ),
            ),
        )
        val buildings = buildingService.getBuildingList(campusID = 1)
        assertEquals(1, buildings.size)
        assertEquals("Building A", buildings[0].name)
        assertEquals(1, buildings[0].campusID)
    }

    @Test
    @DisplayName("건물 목록 조회 테스트 (이름 필터링)")
    fun testGetBuildingListWithName() {
        whenever(buildingRepository.findByNameContaining("A")).thenReturn(
            listOf(
                Building(
                    id = "Y001",
                    name = "Building A",
                    campusID = 1,
                    latitude = 37.5665,
                    longitude = 126.978,
                    url = "http://example.com/building-a",
                ),
            ),
        )
        val buildings = buildingService.getBuildingList(name = "A")
        assertEquals(1, buildings.size)
        assertEquals("Building A", buildings[0].name)
        assertEquals(1, buildings[0].campusID)
    }

    @Test
    @DisplayName("건물 목록 조회 테스트 (캠퍼스 ID와 이름 필터링)")
    fun testGetBuildingListWithCampusIDAndName() {
        whenever(buildingRepository.findByCampusIDAndNameContaining(1, "A")).thenReturn(
            listOf(
                Building(
                    id = "Y001",
                    name = "Building A",
                    campusID = 1,
                    latitude = 37.5665,
                    longitude = 126.978,
                    url = "http://example.com/building-a",
                ),
            ),
        )
        val buildings = buildingService.getBuildingList(campusID = 1, name = "A")
        assertEquals(1, buildings.size)
        assertEquals("Building A", buildings[0].name)
        assertEquals(1, buildings[0].campusID)
    }

    @Test
    @DisplayName("건물 이름으로 조회 테스트")
    fun testGetBuildingByName() {
        val building =
            Building(
                id = "Y001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-a",
            )
        whenever(buildingRepository.findByName("Building A")).thenReturn(building)

        val result = buildingService.getBuildingByName("Building A")
        assertEquals(building, result)
    }

    @Test
    @DisplayName("건물 이름으로 조회 실패 테스트")
    fun testGetBuildingByNameNotFound() {
        whenever(buildingRepository.findByName("Nonexistent Building")).thenReturn(null)
        assertThrows<BuildingNotFoundException> {
            buildingService.getBuildingByName("Nonexistent Building")
        }
    }

    @Test
    @DisplayName("건물 생성 테스트")
    fun testCreateBuilding() {
        val payload =
            CreateBuildingRequest(
                id = "Y003",
                name = "Building C",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-c",
            )

        whenever(buildingRepository.findByName(payload.name)).thenReturn(null)
        whenever(
            buildingRepository.save(
                Building(
                    payload.id,
                    payload.name,
                    payload.campusID,
                    payload.latitude,
                    payload.longitude,
                    payload.url,
                ),
            ),
        ).thenReturn(
            Building(
                id = payload.id,
                name = payload.name,
                campusID = payload.campusID,
                latitude = payload.latitude,
                longitude = payload.longitude,
                url = payload.url,
            ),
        )
        val expectedBuilding = buildingService.createBuilding(payload)
        assertEquals(expectedBuilding.id, payload.id)
    }

    @Test
    @DisplayName("건물 생성 실패 테스트 (중복 이름)")
    fun testCreateBuildingDuplicateName() {
        val payload =
            CreateBuildingRequest(
                id = "Y003",
                name = "Building C",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-c",
            )

        whenever(buildingRepository.findByName(payload.name)).thenReturn(
            Building(
                id = "Y001",
                name = "Building C",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-c",
            ),
        )

        assertThrows<DuplicateBuildingNameException> {
            buildingService.createBuilding(payload)
        }
    }

    @Test
    @DisplayName("건물 수정 테스트")
    fun testUpdateBuilding() {
        val existingBuilding =
            Building(
                id = "Y001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-a",
            )
        val payload =
            UpdateBuildingRequest(
                id = "Y001",
                campusID = 2,
                latitude = 37.5670,
                longitude = 126.979,
                url = "http://example.com/building-a-updated",
            )

        whenever(buildingRepository.findById("Building A")).thenReturn(Optional.of(existingBuilding))
        whenever(buildingRepository.save(existingBuilding)).thenReturn(
            existingBuilding.apply {
                id = payload.id
                campusID = payload.campusID
                latitude = payload.latitude
                longitude = payload.longitude
                url = payload.url
            },
        )

        buildingService.updateBuilding("Building A", payload)
        assertEquals(2, existingBuilding.campusID)
        assertEquals(37.5670, existingBuilding.latitude)
        assertEquals(126.979, existingBuilding.longitude)
        assertEquals("http://example.com/building-a-updated", existingBuilding.url)
    }

    @Test
    @DisplayName("건물 수정 실패 테스트 (존재하지 않는 건물)")
    fun testUpdateBuildingNotFound() {
        val payload =
            UpdateBuildingRequest(
                id = "Y001",
                campusID = 2,
                latitude = 37.5670,
                longitude = 126.979,
                url = "http://example.com/building-a-updated",
            )

        whenever(buildingRepository.findById("Nonexistent Building")).thenReturn(Optional.empty())

        assertThrows<BuildingNotFoundException> {
            buildingService.updateBuilding("Nonexistent Building", payload)
        }
    }

    @Test
    @DisplayName("건물 삭제 테스트")
    fun testDeleteBuildingByName() {
        val existingBuilding =
            Building(
                id = "Y001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-a",
            )

        whenever(buildingRepository.findByName("Building A")).thenReturn(existingBuilding)
        buildingService.deleteBuildingByName("Building A")
        // Verify that the delete method was called
        verify(buildingRepository).delete(existingBuilding)
    }

    @Test
    @DisplayName("건물 삭제 실패 테스트 (존재하지 않는 건물)")
    fun testDeleteBuildingByNameNotFound() {
        whenever(buildingRepository.findByName("Nonexistent Building")).thenReturn(null)
        assertThrows<BuildingNotFoundException> {
            buildingService.deleteBuildingByName("Nonexistent Building")
        }
    }
}
