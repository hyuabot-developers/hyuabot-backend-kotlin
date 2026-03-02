package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Contact
import app.hyuabot.backend.database.entity.ContactCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ContactRepository : JpaRepository<Contact, Int> {
    fun findByCategoryID(categoryID: Int): List<Contact>

    fun findByCampusID(campusID: Int): List<Contact>

    fun findByNameContainingOrPhoneContains(
        name: String,
        phone: String,
    ): List<Contact>

    @Query(
        "SELECT DISTINCT cc FROM contact_category cc " +
            "LEFT JOIN contact c ON cc.id = c.categoryID",
    )
    fun findAllCategoryWithContact(): List<ContactCategory>
}
