package app.hyuabot.backend.cafeteria.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateCafeteriaIDException : DuplicateKeyException {
    constructor() : super("DUPLICATE_CAFETERIA_ID")
}
