package app.hyuabot.backend.shuttle.exception

class ShuttleInitialStopRuleNotFoundException : RuntimeException()

class InvalidShuttleInitialStopRuleException(
    message: String,
) : IllegalArgumentException(message)
