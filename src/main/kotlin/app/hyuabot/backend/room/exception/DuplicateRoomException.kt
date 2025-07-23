package app.hyuabot.backend.room.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateRoomException : DuplicateKeyException {
    constructor() : super("DUPLICATE_BUILDING_ROOM")
}
