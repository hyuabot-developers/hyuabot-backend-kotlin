package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ContactCategory
import org.springframework.data.jpa.repository.JpaRepository

interface ContactCategoryRepository : JpaRepository<ContactCategory, Int> {
    fun findByNameContaining(name: String): List<ContactCategory>

    fun findByName(name: String): ContactCategory?
}
