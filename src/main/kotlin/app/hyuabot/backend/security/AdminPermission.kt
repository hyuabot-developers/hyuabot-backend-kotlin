package app.hyuabot.backend.security

enum class AdminPermission {
    SUPER_ADMIN,
    SHUTTLE,
    BUS,
    SUBWAY,
    CAFETERIA,
    READING_ROOM,
    CONTACT,
    CALENDAR,
    NOTICE,
    INQUIRY,
    ;

    companion object {
        val managementPermissions: Set<AdminPermission> = entries.filterNot { it == SUPER_ADMIN }.toSet()
    }
}

fun Set<AdminPermission>.effectivePermissions(): Set<AdminPermission> =
    if (contains(AdminPermission.SUPER_ADMIN)) AdminPermission.entries.toSet() else this
