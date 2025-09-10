package app.hyuabot.backend.subway.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateSubwayRouteException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SUBWAY_ROUTE")
}
