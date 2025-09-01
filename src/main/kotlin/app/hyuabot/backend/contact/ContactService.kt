package app.hyuabot.backend.contact

import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.contact.domain.ContactCategoryRequest
import app.hyuabot.backend.contact.domain.ContactRequest
import app.hyuabot.backend.contact.exception.ContactCategoryNotFoundException
import app.hyuabot.backend.contact.exception.ContactNotFoundException
import app.hyuabot.backend.contact.exception.ContactVersionNotFoundException
import app.hyuabot.backend.contact.exception.DuplicateCategoryException
import app.hyuabot.backend.database.entity.Contact
import app.hyuabot.backend.database.entity.ContactCategory
import app.hyuabot.backend.database.entity.ContactVersion
import app.hyuabot.backend.database.exception.PhoneNumberNotValidException
import app.hyuabot.backend.database.repository.CampusRepository
import app.hyuabot.backend.database.repository.ContactCategoryRepository
import app.hyuabot.backend.database.repository.ContactRepository
import app.hyuabot.backend.database.repository.ContactVersionRepository
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class ContactService(
    private val campusRepository: CampusRepository,
    private val categoryRepository: ContactCategoryRepository,
    private val contactRepository: ContactRepository,
    private val versionRepository: ContactVersionRepository,
) {
    fun getContactVersion(): ContactVersion =
        versionRepository.findTopByOrderByCreatedAtDesc()
            ?: throw ContactVersionNotFoundException()

    fun getContactCategories(): List<ContactCategory> = categoryRepository.findAll().sortedBy { it.id }

    fun createContactCategory(payload: ContactCategoryRequest): ContactCategory {
        categoryRepository.findByName(payload.name)?.let {
            throw DuplicateCategoryException()
        }
        updateContactVersion()
        return categoryRepository.save(
            ContactCategory(
                name = payload.name,
                contact = emptyList(),
            ),
        )
    }

    fun getContactCategoryById(id: Int): ContactCategory =
        categoryRepository.findById(id).orElseThrow { ContactCategoryNotFoundException() }

    fun deleteContactCategoryById(id: Int) {
        categoryRepository.findById(id).orElseThrow { ContactCategoryNotFoundException() }.let { category ->
            // 해당 카테고리에 속한 연락처도 모두 삭제
            contactRepository.deleteAll(category.contact)
            categoryRepository.delete(category)
            updateContactVersion()
        }
    }

    fun getAllContacts(): List<Contact> = contactRepository.findAll().sortedBy { it.id }

    fun getContactByCategoryId(id: Int): List<Contact> {
        categoryRepository.findById(id).orElseThrow { ContactCategoryNotFoundException() }.let { category ->
            return category.contact.sortedBy { it.id }
        }
    }

    fun createContact(payload: ContactRequest): Contact {
        if (!checkPhoneNumberFormat(payload.phone)) {
            throw PhoneNumberNotValidException()
        }
        campusRepository
            .findById(payload.campusID)
            .orElseThrow { CampusNotFoundException() }
        categoryRepository
            .findById(payload.categoryID)
            .orElseThrow { ContactCategoryNotFoundException() }
        updateContactVersion()
        return contactRepository.save(
            Contact(
                campusID = payload.campusID,
                name = payload.name,
                phone = payload.phone,
                categoryID = payload.categoryID,
                category = null,
            ),
        )
    }

    fun getContactById(id: Int): Contact = contactRepository.findById(id).orElseThrow { ContactNotFoundException() }

    fun updateContact(
        id: Int,
        payload: ContactRequest,
    ): Contact {
        if (!checkPhoneNumberFormat(payload.phone)) {
            throw PhoneNumberNotValidException()
        }
        campusRepository
            .findById(payload.campusID)
            .orElseThrow { CampusNotFoundException() }
        categoryRepository
            .findById(payload.categoryID)
            .orElseThrow { ContactCategoryNotFoundException() }
        updateContactVersion()
        return contactRepository.findById(id).orElseThrow { ContactNotFoundException() }.let { contact ->
            contact.apply {
                name = payload.name
                phone = payload.phone
                campusID = payload.campusID
                categoryID = payload.categoryID
            }
            contactRepository.save(contact)
        }
    }

    fun deleteContactById(id: Int) {
        contactRepository.findById(id).orElseThrow { ContactNotFoundException() }.let { contact ->
            contactRepository.delete(contact)
            updateContactVersion()
        }
    }

    fun updateContactVersion() {
        val now = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        val versionName = now.format(dateTimeFormatter)
        val current = versionRepository.findTopByOrderByCreatedAtDesc()
        if (current == null) {
            versionRepository.save(
                ContactVersion(
                    name = versionName,
                    createdAt = now,
                ),
            )
        } else {
            current.name = versionName
            current.createdAt = now
            versionRepository.save(current)
        }
    }

    companion object {
        private val phoneRegex = Regex("^\\d{2,3}-\\d{3,4}-\\d{4}$")
        private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun checkPhoneNumberFormat(phone: String): Boolean = phoneRegex.matches(phone)
    }
}
