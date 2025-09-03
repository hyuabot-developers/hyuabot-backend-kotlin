package app.hyuabot.backend.notice.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateCategoryException : DuplicateKeyException {
    constructor() : super("DUPLICATE_CATEGORY_NAME")
}
