package app.hyuabot.backend.campus

import app.hyuabot.backend.campus.domain.CreateCampusRequest
import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.campus.exception.DuplicateCampusException
import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.repository.CampusRepository
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
class CampusServiceTest {
    @Mock
    lateinit var campusRepository: CampusRepository

    @InjectMocks
    lateinit var campusService: CampusService

    @Test
    @DisplayName("캠퍼스 목록 테스트")
    fun testGetCampusList() {
        whenever(campusRepository.findAll()).thenReturn(
            listOf(
                Campus(id = 1, name = "서울"),
                Campus(id = 2, name = "ERICA"),
            ),
        )
        val campusList = campusService.getCampusList()
        assertEquals(2, campusList.size)
        assertEquals("서울", campusList[0].name)
        assertEquals("ERICA", campusList[1].name)
        assertEquals(1, campusList[0].id)
        assertEquals(2, campusList[1].id)
    }

    @Test
    @DisplayName("캠퍼스 ID로 조회 테스트")
    fun testGetCampusById() {
        val campus = Campus(id = 1, name = "서울")
        whenever(campusRepository.findById(1)).thenReturn(Optional.of(campus))

        val foundCampus = campusService.getCampusById(1)
        assertEquals(foundCampus, campus)
    }

    @Test
    @DisplayName("캠퍼스 ID로 조회 실패 테스트")
    fun testGetCampusByIdNotFound() {
        whenever(campusRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<CampusNotFoundException> { campusService.getCampusById(999) }
    }

    @Test
    @DisplayName("캠퍼스 ID로 수정 테스트")
    fun testUpdateCampus() {
        val existingCampus = Campus(id = 1, name = "서울")
        whenever(campusRepository.findById(1)).thenReturn(Optional.of(existingCampus))
        whenever(campusRepository.save(existingCampus)).thenReturn(existingCampus)

        val updatedCampus = campusService.updateCampus(1, CreateCampusRequest(name = "서울 수정"))
        assertEquals("서울 수정", updatedCampus.name)
        assertEquals(1, updatedCampus.id)
    }

    @Test
    @DisplayName("캠퍼스 ID로 수정 테스트 (동일 이름)")
    fun testUpdateCampusSameName() {
        val existingCampus = Campus(id = 1, name = "서울")
        whenever(campusRepository.findById(1)).thenReturn(Optional.of(existingCampus))
        whenever(campusRepository.findByName("서울")).thenReturn(listOf(existingCampus))
        whenever(campusRepository.save(existingCampus)).thenReturn(existingCampus)

        val updatedCampus = campusService.updateCampus(1, CreateCampusRequest(name = "서울"))
        assertEquals("서울", updatedCampus.name)
        assertEquals(1, updatedCampus.id)
    }

    @Test
    @DisplayName("캠퍼스 ID로 수정 실패 테스트(중복 이름)")
    fun testUpdateCampusDuplicateName() {
        val existingCampus = Campus(id = 1, name = "서울")
        whenever(campusRepository.findById(1)).thenReturn(Optional.of(existingCampus))
        whenever(campusRepository.findByName("서울 수정")).thenReturn(listOf(Campus(id = 2, name = "서울 수정")))

        assertThrows<DuplicateCampusException> {
            campusService.updateCampus(1, CreateCampusRequest(name = "서울 수정"))
        }
    }

    @Test
    @DisplayName("캠퍼스 ID로 수정 실패 테스트(캠퍼스 없음)")
    fun testUpdateCampusNotFound() {
        whenever(campusRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<CampusNotFoundException> { campusService.updateCampus(999, CreateCampusRequest(name = "서울 수정")) }
    }

    @Test
    @DisplayName("캠퍼스 ID로 삭제 테스트")
    fun testDeleteCampusById() {
        val campus = Campus(id = 1, name = "서울")
        whenever(campusRepository.findById(1)).thenReturn(Optional.of(campus))

        campusService.deleteCampusById(1)

        // Verify that the delete method was called
        verify(campusRepository).delete(campus)
    }

    @Test
    @DisplayName("캠퍼스 ID로 삭제 실패 테스트(캠퍼스 없음)")
    fun testDeleteCampusByIdNotFound() {
        whenever(campusRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<CampusNotFoundException> { campusService.deleteCampusById(999) }
    }

    @Test
    @DisplayName("캠퍼스 생성 테스트")
    fun testCreateCampus() {
        val newCampus = CreateCampusRequest(name = "서울")
        whenever(campusRepository.findByName("서울")).thenReturn(emptyList())
        whenever(campusRepository.save(Campus(name = "서울"))).thenReturn(Campus(id = 1, name = "서울"))

        val createdCampus = campusService.createCampus(newCampus)
        assertEquals("서울", createdCampus.name)
        assertEquals(1, createdCampus.id)
    }

    @Test
    @DisplayName("캠퍼스 생성 실패 테스트(중복 이름)")
    fun testCreateCampusDuplicateName() {
        val newCampus = CreateCampusRequest(name = "서울")
        whenever(campusRepository.findByName("서울")).thenReturn(listOf(Campus(id = 1, name = "서울")))

        assertThrows<DuplicateCampusException> { campusService.createCampus(newCampus) }
    }
}
