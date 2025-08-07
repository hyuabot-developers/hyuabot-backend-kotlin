package app.hyuabot.backend.shuttle.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateShuttleRouteStopException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SHUTTLE_ROUTE_STOP")
}
