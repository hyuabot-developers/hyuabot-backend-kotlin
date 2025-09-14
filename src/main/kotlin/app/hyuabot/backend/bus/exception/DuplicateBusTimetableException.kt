package app.hyuabot.backend.bus.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateBusTimetableException : DuplicateKeyException {
    constructor() : super("DUPLICATE_BUS_TIMETABLE")
}
