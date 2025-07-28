package app.hyuabot.backend.readingRoom.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateReadingRoomException : DuplicateKeyException {
    constructor() : super("DUPLICATE_READING_ROOM_ID")
}
