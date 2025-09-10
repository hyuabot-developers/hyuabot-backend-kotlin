package app.hyuabot.backend.subway.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateSubwayStationException : DuplicateKeyException {
    constructor() : super("DUPLICATE_SUBWAY_STATION")
}
