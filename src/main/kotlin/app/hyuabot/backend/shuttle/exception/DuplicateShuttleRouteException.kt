package app.hyuabot.backend.shuttle.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateShuttleRouteException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SHUTTLE_ROUTE")
}
