package app.hyuabot.backend.bus.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateBusStopException : DuplicateKeyException {
    constructor() : super("DUPLICATE_BUS_STOP")
}
