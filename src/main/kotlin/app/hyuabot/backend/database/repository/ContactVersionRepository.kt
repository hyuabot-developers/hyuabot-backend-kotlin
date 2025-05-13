package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ContactVersion
import org.springframework.data.jpa.repository.JpaRepository

interface ContactVersionRepository : JpaRepository<ContactVersion, Int>
