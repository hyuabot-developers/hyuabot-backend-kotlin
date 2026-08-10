package app.hyuabot.backend.subway

import app.hyuabot.backend.database.repository.SubwayStationTranslationRepository
import app.hyuabot.backend.subway.service.SubwayStationNameService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class SubwayStationNameServiceTest {
    @Mock
    lateinit var repository: SubwayStationTranslationRepository

    @ParameterizedTest
    @CsvSource(
        "zh-Hant-TW, zh-Hant",
        "zh-HK, zh-Hant",
        "zh-CN, zh-Hans",
        "en-US, en",
        "ja-JP, ja",
        "ko-KR, ko",
        "'', ko",
    )
    fun `normalizes supported language tags`(
        language: String,
        expected: String,
    ) {
        assertEquals(expected, SubwayStationNameService(repository).normalizeLanguage(language))
    }

    @Test
    fun `defaults a missing language to Korean`() {
        assertEquals("ko", SubwayStationNameService(repository).normalizeLanguage(null))
    }

    @Test
    fun `normalizes regional language tags and returns translated name`() {
        val service = SubwayStationNameService(repository)
        whenever(repository.findName("K453", "zh-Hant")).thenReturn("安山")

        assertEquals("安山", service.displayName("K453", "zh_TW", "안산"))
    }

    @Test
    fun `falls back to Korean and then source name`() {
        val service = SubwayStationNameService(repository)
        whenever(repository.findName("K453", "ja")).thenReturn(null)
        whenever(repository.findName("K453", "ko")).thenReturn("안산")
        assertEquals("안산", service.displayName("K453", "ja-JP", "fallback"))

        whenever(repository.findName("S07", "en")).thenReturn(null)
        whenever(repository.findName("S07", "ko")).thenReturn(null)
        assertEquals("일산", service.displayName("S07", "en-US", "일산"))
    }

    @Test
    fun `returns translated name from Korean realtime location`() {
        val service = SubwayStationNameService(repository)
        whenever(repository.findNameByKoreanName("중앙", "en")).thenReturn("Jungang")

        assertEquals("Jungang", service.displayNameByKoreanName("중앙", "en-US"))
    }

    @Test
    fun `falls back to source realtime location`() {
        val service = SubwayStationNameService(repository)
        whenever(repository.findNameByKoreanName("임시역", "ja")).thenReturn(null)
        whenever(repository.findNameByKoreanName("임시역", "ko")).thenReturn(null)

        assertEquals("임시역", service.displayNameByKoreanName("임시역", "ja-JP"))
    }

    @Test
    fun `falls back to Korean realtime location translation`() {
        val service = SubwayStationNameService(repository)
        whenever(repository.findNameByKoreanName("중앙", "ja")).thenReturn(null)
        whenever(repository.findNameByKoreanName("중앙", "ko")).thenReturn("중앙")

        assertEquals("중앙", service.displayNameByKoreanName("중앙", "ja-JP"))
    }
}
