package app.hyuabot.backend.auth.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateUserIDException : DuplicateKeyException {
    constructor() : super("DUPLICATE_USER_ID")
}
