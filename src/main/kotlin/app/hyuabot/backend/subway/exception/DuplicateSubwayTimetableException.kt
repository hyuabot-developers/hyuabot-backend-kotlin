package app.hyuabot.backend.subway.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateSubwayTimetableException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SHUTTLE_TIMETABLE")
}
