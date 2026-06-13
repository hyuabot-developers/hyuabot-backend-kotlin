package app.hyuabot.backend.holiday.exception

import org.springframework.dao.DuplicateKeyException

class DuplicatePublicHolidayException : DuplicateKeyException {
    constructor() : super("DUPLICATE_PUBLIC_HOLIDAY")
}
