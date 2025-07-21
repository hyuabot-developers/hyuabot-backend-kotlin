package app.hyuabot.backend.campus.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateCampusException : DuplicateKeyException {
    constructor() : super("DUPLICATE_CAMPUS_NAME")
}
