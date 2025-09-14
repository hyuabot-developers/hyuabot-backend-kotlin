package app.hyuabot.backend.bus.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateBusRouteException : DuplicateKeyException {
    constructor() : super("DUPLICATE_BUS_ROUTE")
}
