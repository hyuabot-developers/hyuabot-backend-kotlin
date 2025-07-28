package app.hyuabot.backend.building.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateBuildingNameException : DuplicateKeyException {
    constructor() : super("DUPLICATE_BUILDING_NAME")
}
