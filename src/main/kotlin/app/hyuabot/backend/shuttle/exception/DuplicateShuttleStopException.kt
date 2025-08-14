package app.hyuabot.backend.shuttle.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateShuttleStopException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SHUTTLE_STOP")
}
