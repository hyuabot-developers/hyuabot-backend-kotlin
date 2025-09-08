package app.hyuabot.backend.commuteShuttle.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateShuttleRouteException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SHUTTLE_ROUTE")
}
