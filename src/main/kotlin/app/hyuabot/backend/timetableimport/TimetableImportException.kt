package app.hyuabot.backend.timetableimport

class TimetableImportException(
    val code: String,
) : IllegalStateException(code)
