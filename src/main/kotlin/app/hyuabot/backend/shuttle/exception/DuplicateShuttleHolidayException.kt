package app.hyuabot.backend.shuttle.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateShuttleHolidayException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SHUTTLE_HOLIDAY")
}
