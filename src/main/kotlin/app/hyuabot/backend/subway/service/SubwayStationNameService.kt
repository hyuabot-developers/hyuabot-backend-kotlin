package app.hyuabot.backend.subway.service

import app.hyuabot.backend.database.repository.SubwayStationTranslationRepository
import org.springframework.stereotype.Service

@Service
class SubwayStationNameService(
    private val repository: SubwayStationTranslationRepository,
) {
    fun displayName(
        stationID: String,
        language: String?,
        fallback: String,
    ): String {
        val normalizedLanguage = normalizeLanguage(language)
        return repository.findName(stationID, normalizedLanguage)
            ?: repository.findName(stationID, DEFAULT_LANGUAGE)
            ?: fallback
    }

    internal fun normalizeLanguage(language: String?): String {
        val tag =
            language
                .orEmpty()
                .trim()
                .replace('_', '-')
                .lowercase()
        return when {
            tag.startsWith("zh-hant") || tag.startsWith("zh-tw") || tag.startsWith("zh-hk") -> "zh-Hant"
            tag.startsWith("zh") -> "zh-Hans"
            tag.startsWith("en") -> "en"
            tag.startsWith("ja") -> "ja"
            else -> DEFAULT_LANGUAGE
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "ko"
    }
}
