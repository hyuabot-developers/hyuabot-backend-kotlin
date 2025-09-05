package app.hyuabot.backend.commuteShuttle.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateShuttleStopException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SHUTTLE_STOP")
}
