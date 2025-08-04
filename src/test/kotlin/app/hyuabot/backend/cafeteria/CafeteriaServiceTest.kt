package app.hyuabot.backend.cafeteria

import app.hyuabot.backend.cafeteria.domain.CreateCafeteriaRequest
import app.hyuabot.backend.cafeteria.domain.UpdateCafeteriaRequest
import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.cafeteria.exception.DuplicateCafeteriaIDException
import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.database.entity.Cafeteria
import app.hyuabot.backend.database.repository.CafeteriaRepository
import app.hyuabot.backend.database.repository.CampusRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CafeteriaServiceTest {
    @Mock
    lateinit var cafeteriaRepository: CafeteriaRepository

    @Mock
    lateinit var campusRepository: CampusRepository

    @InjectMocks
    lateinit var cafeteriaService: CafeteriaService

    @Test
    @DisplayName("학식 식당 목록 조회 테스트")
    fun testGetCafeteriaList() {
        whenever(cafeteriaRepository.findAll()).thenReturn(
            listOf(
                Cafeteria(
                    id = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                    campusID = 1,
                    campus = null,
                ),
                Cafeteria(
                    id = 2,
                    name = "Cafeteria B",
                    latitude = 37.7750,
                    longitude = -122.4195,
                    breakfastTime = "07:30-10:30",
                    lunchTime = "11:30-14:30",
                    dinnerTime = "17:30-20:30",
                    campusID = 1,
                    campus = null,
                ),
                Cafeteria(
                    id = 3,
                    name = "Cafeteria C",
                    latitude = 37.7751,
                    longitude = -122.4196,
                    breakfastTime = "08:00-11:00",
                    lunchTime = "12:00-15:00",
                    dinnerTime = "18:00-21:00",
                    campusID = 2,
                    campus = null,
                ),
            ),
        )
        val cafeterias = cafeteriaService.getCafeteriaList()
        assertEquals(3, cafeterias.size)
        assertEquals("Cafeteria A", cafeterias[0].name)
        assertEquals("Cafeteria B", cafeterias[1].name)
        assertEquals("Cafeteria C", cafeterias[2].name)
    }

    @Test
    @DisplayName("학식 식당 목록 조회 (캠퍼스 ID 필터링) 테스트")
    fun testGetCafeteriaListByCampusID() {
        whenever(cafeteriaRepository.findByCampusID(1)).thenReturn(
            listOf(
                Cafeteria(
                    id = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                    campusID = 1,
                    campus = null,
                ),
                Cafeteria(
                    id = 2,
                    name = "Cafeteria B",
                    latitude = 37.7750,
                    longitude = -122.4195,
                    breakfastTime = "07:30-10:30",
                    lunchTime = "11:30-14:30",
                    dinnerTime = "17:30-20:30",
                    campusID = 1,
                    campus = null,
                ),
            ),
        )
        val cafeterias = cafeteriaService.getCafeteriaList(campusID = 1)
        assertEquals(2, cafeterias.size)
        assertEquals("Cafeteria A", cafeterias[0].name)
        assertEquals("Cafeteria B", cafeterias[1].name)
    }

    @Test
    @DisplayName("학식 식당 목록 조회 (이름 필터링) 테스트")
    fun testGetCafeteriaListByName() {
        whenever(cafeteriaRepository.findByNameContaining("A")).thenReturn(
            listOf(
                Cafeteria(
                    id = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                    campusID = 1,
                    campus = null,
                ),
            ),
        )
        val cafeterias = cafeteriaService.getCafeteriaList(name = "A")
        assertEquals(1, cafeterias.size)
        assertEquals("Cafeteria A", cafeterias[0].name)
    }

    @Test
    @DisplayName("학식 식당 목록 조회 (캠퍼스 ID와 이름 필터링) 테스트")
    fun testGetCafeteriaListByCampusIDAndName() {
        whenever(cafeteriaRepository.findByCampusIDAndNameContaining(1, "A")).thenReturn(
            listOf(
                Cafeteria(
                    id = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                    campusID = 1,
                    campus = null,
                ),
            ),
        )
        val cafeterias = cafeteriaService.getCafeteriaList(campusID = 1, name = "A")
        assertEquals(1, cafeterias.size)
        assertEquals("Cafeteria A", cafeterias[0].name)
    }

    @Test
    @DisplayName("학식 식당 ID로 조회 테스트")
    fun testGetCafeteriaById() {
        val cafeteria =
            Cafeteria(
                id = 1,
                name = "Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campusID = 1,
                campus = null,
            )
        whenever(cafeteriaRepository.findById(1)).thenReturn(Optional.of(cafeteria))
        val foundCafeteria = cafeteriaService.getCafeteriaById(1)
        assertEquals(cafeteria, foundCafeteria)
    }

    @Test
    @DisplayName("학식 식당 ID로 조회 실패 테스트")
    fun testGetCafeteriaByIdNotFound() {
        whenever(cafeteriaRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<CafeteriaNotFoundException> { cafeteriaService.getCafeteriaById(1) }
    }

    @Test
    @DisplayName("학식 식당 생성 테스트")
    fun testCreateCafeteria() {
        val newCafeteria =
            Cafeteria(
                id = 1,
                name = "Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campusID = 1,
                campus = null,
            )
        whenever(cafeteriaRepository.save(newCafeteria)).thenReturn(newCafeteria)
        whenever(campusRepository.existsById(1)).thenReturn(true)

        val createdCafeteria =
            cafeteriaService.createCafeteria(
                CreateCafeteriaRequest(
                    id = 1,
                    campusID = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                ),
            )
        assertEquals(newCafeteria, createdCafeteria)
    }

    @Test
    @DisplayName("학식 식당 생성 실패 테스트 (중복 ID)")
    fun testCreateCafeteriaDuplicateID() {
        whenever(cafeteriaRepository.existsById(1)).thenReturn(true)
        assertThrows<DuplicateCafeteriaIDException> {
            cafeteriaService.createCafeteria(
                CreateCafeteriaRequest(
                    id = 1,
                    campusID = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("학식 식당 생성 실패 테스트 (존재하지 않는 캠퍼스 ID)")
    fun testCreateCafeteriaNonExistentCampus() {
        whenever(cafeteriaRepository.existsById(1)).thenReturn(false)
        whenever(campusRepository.existsById(1)).thenReturn(false)
        assertThrows<CampusNotFoundException> {
            cafeteriaService.createCafeteria(
                CreateCafeteriaRequest(
                    id = 1,
                    campusID = 1,
                    name = "Cafeteria A",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    breakfastTime = "07:00-10:00",
                    lunchTime = "11:00-14:00",
                    dinnerTime = "17:00-20:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("학식 식당 수정 테스트")
    fun testUpdateCafeteria() {
        val existingCafeteria =
            Cafeteria(
                id = 1,
                name = "Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campusID = 1,
                campus = null,
            )
        whenever(cafeteriaRepository.findById(1)).thenReturn(Optional.of(existingCafeteria))
        whenever(campusRepository.existsById(1)).thenReturn(true)
        whenever(cafeteriaRepository.save(existingCafeteria)).thenReturn(
            existingCafeteria.copy(name = "Updated Cafeteria A", breakfastTime = "07:30-10:30"),
        )

        val updatedCafeteria =
            cafeteriaService.updateCafeteria(
                id = 1,
                payload =
                    UpdateCafeteriaRequest(
                        campusID = 1,
                        name = "Updated Cafeteria A",
                        latitude = 37.7749,
                        longitude = -122.4194,
                        breakfastTime = "07:30-10:30",
                        lunchTime = "11:30-14:30",
                        dinnerTime = "17:30-20:30",
                    ),
            )
        assertEquals("Updated Cafeteria A", updatedCafeteria.name)
        assertEquals("07:30-10:30", updatedCafeteria.breakfastTime)
    }

    @Test
    @DisplayName("학식 식당 수정 실패 테스트 (존재하지 않는 캠퍼스 ID)")
    fun testUpdateCafeteriaNonExistentCampus() {
        val existingCafeteria =
            Cafeteria(
                id = 1,
                name = "Cafeteria A",
                latitude = 37.7749,
                longitude = -122.4194,
                breakfastTime = "07:00-10:00",
                lunchTime = "11:00-14:00",
                dinnerTime = "17:00-20:00",
                campusID = 1,
                campus = null,
            )
        whenever(cafeteriaRepository.findById(1)).thenReturn(Optional.of(existingCafeteria))
        whenever(campusRepository.existsById(2)).thenReturn(false)

        assertThrows<CampusNotFoundException> {
            cafeteriaService.updateCafeteria(
                id = 1,
                payload =
                    UpdateCafeteriaRequest(
                        campusID = 2,
                        name = "Updated Cafeteria A",
                        latitude = 37.7749,
                        longitude = -122.4194,
                        breakfastTime = "07:30-10:30",
                        lunchTime = "11:30-14:30",
                        dinnerTime = "17:30-20:30",
                    ),
            )
        }
    }

    @Test
    @DisplayName("학식 수정 실패 테스트 (식당 ID가 존재하지 않음)")
    fun testUpdateCafeteriaNotFound() {
        whenever(cafeteriaRepository.findById(1)).thenReturn(Optional.empty())
        assertThrows<CafeteriaNotFoundException> {
            cafeteriaService.updateCafeteria(
                id = 1,
                payload =
                    UpdateCafeteriaRequest(
                        campusID = 1,
                        name = "Updated Cafeteria A",
                        latitude = 37.7749,
                        longitude = -122.4194,
                        breakfastTime = "07:30-10:30",
                        lunchTime = "11:30-14:30",
                        dinnerTime = "17:30-20:30",
                    ),
            )
        }
    }

    @Test
    @DisplayName("학식 식당 삭제 테스트")
    fun testDeleteCafeteriaById() {
        whenever(cafeteriaRepository.existsById(1)).thenReturn(true)
        cafeteriaService.deleteCafeteriaById(1)
    }

    @Test
    @DisplayName("학식 식당 삭제 실패 테스트 (식당 ID가 존재하지 않음)")
    fun testDeleteCafeteriaByIdNotFound() {
        whenever(cafeteriaRepository.existsById(1)).thenReturn(false)
        assertThrows<CafeteriaNotFoundException> { cafeteriaService.deleteCafeteriaById(1) }
    }
}
