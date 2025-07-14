package app.hyuabot.backend.auth.exception

import org.springframework.dao.DuplicateKeyException

class DuplicateEmailException : DuplicateKeyException("DUPLICATE_EMAIL")
