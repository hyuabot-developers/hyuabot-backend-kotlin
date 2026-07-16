package app.hyuabot.backend.timetableimport

import java.security.MessageDigest

object TimetableImportSupport {
    fun fingerprint(values: List<String>): String {
        val canonical = values.sorted().joinToString("\n")
        return MessageDigest
            .getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
