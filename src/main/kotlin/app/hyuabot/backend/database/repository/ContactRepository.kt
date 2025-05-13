package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Contact
import org.springframework.data.jpa.repository.JpaRepository

interface ContactRepository : JpaRepository<Contact, Int> {
    fun findByCategoryID(categoryID: Int): List<Contact>

    fun findByCampusID(campusID: Int): List<Contact>

    fun findByNameContainingOrPhoneContains(
        name: String,
        phone: String,
    ): List<Contact>
}
