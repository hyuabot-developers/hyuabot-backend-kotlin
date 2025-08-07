package app.hyuabot.backend.shuttle.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateShuttleTimetableException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SHUTTLE_TIMETABLE")
}
