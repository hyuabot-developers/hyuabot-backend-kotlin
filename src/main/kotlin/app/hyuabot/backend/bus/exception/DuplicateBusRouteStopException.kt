package app.hyuabot.backend.bus.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateBusRouteStopException : DuplicateKeyException {
    constructor() : super("DUPLICATE_BUS_ROUTE_STOP")
}
