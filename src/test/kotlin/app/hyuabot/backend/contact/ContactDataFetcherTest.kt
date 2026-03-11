package app.hyuabot.backend.contact

import app.hyuabot.backend.contact.controller.ContactDataFetcher
import app.hyuabot.backend.database.entity.Contact
import app.hyuabot.backend.database.entity.ContactCategory
import app.hyuabot.backend.database.entity.ContactVersion
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

@EnableDgsTest
@SpringJUnitConfig(ContactDataFetcher::class)
@Import(ContactDataFetcher::class, ScalarRegistration::class)
class ContactDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var contactService: ContactService

    private fun createContact(
        id: Int = 1,
        campusID: Int = 1,
        categoryID: Int = 1,
        name: String = "TEST",
        phone: String = "031-0000-0000",
    ) = Contact(
        id = id,
        campusID = campusID,
        categoryID = categoryID,
        name = name,
        phone = phone,
        category = null,
    )

    private fun createCategory(
        id: Int = 1,
        name: String = "TEST",
        contacts: MutableList<Contact> = mutableListOf(),
    ) = ContactCategory(
        id = id,
        name = name,
        contact = contacts,
    )

    private val now = ZonedDateTime.now()

    @Test
    @DisplayName("전화부 카테고리와 전화부를 올바르게 반환하는지 테스트")
    fun testFetchContacts() {
        whenever(contactService.getContactVersion()).thenReturn(
            ContactVersion(name = "1.0", createdAt = now),
        )
        whenever(
            contactService.fetchContacts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(
            listOf(
                createCategory(
                    contacts =
                        mutableListOf(
                            createContact(
                                id = 1,
                                name = "TEST",
                                phone = "031-0000-0000",
                            ),
                            createContact(
                                id = 2,
                                name = "TEST2",
                                phone = "031-0000-0001",
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                """
                {
                    phonebook {
                        version
                        categories {
                            seq
                            name
                            entries {
                                seq
                                name
                                phone
                            }
                        }
                    }
                 }
                """.trimIndent(),
                "data.phonebook",
            )
        assertEquals("1.0", result["version"])
        val categories = result["categories"] as List<*>
        val category = categories[0] as Map<*, *>
        assertEquals(1, category["seq"])
        assertEquals("TEST", category["name"])
        val entries = category["entries"] as List<*>
        val entry1 = entries[0] as Map<*, *>
        assertEquals("TEST", entry1["name"])
        assertEquals("031-0000-0000", entry1["phone"])
        val entry2 = entries[1] as Map<*, *>
        assertEquals("TEST2", entry2["name"])
        assertEquals("031-0000-0001", entry2["phone"])
    }

    @Test
    @DisplayName("전화부를 카테고리로 필터링하는지 테스트")
    fun testFetchContactsWithCategoryFilter() {
        whenever(contactService.getContactVersion()).thenReturn(
            ContactVersion(name = "1.0", createdAt = now),
        )
        whenever(
            contactService.fetchContacts(
                eq("TEST"),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(
            listOf(
                createCategory(
                    id = 1,
                    name = "TEST",
                    contacts =
                        mutableListOf(
                            createContact(
                                id = 1,
                                name = "TEST",
                                phone = "031-0000-0000",
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                """
                {
                    phonebook(input: { category: "TEST" }) {
                        version
                        categories {
                            seq
                            name
                            entries {
                                seq
                                name
                                phone
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.phonebook",
            )
        assertEquals("1.0", result["version"])
        val categories = result["categories"] as List<*>
        val category = categories[0] as Map<*, *>
        assertEquals(1, category["seq"])
        assertEquals("TEST", category["name"])
        val entries = category["entries"] as List<*>
        val entry1 = entries[0] as Map<*, *>
        assertEquals("TEST", entry1["name"])
        assertEquals("031-0000-0000", entry1["phone"])
    }

    @Test
    @DisplayName("전화부를 캠퍼스로 필터링하는지 테스트")
    fun testFetchContactsWithCampusFilter() {
        whenever(contactService.getContactVersion()).thenReturn(
            ContactVersion(name = "1.0", createdAt = now),
        )
        whenever(
            contactService.fetchContacts(
                anyOrNull(),
                eq(1),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(
            listOf(
                createCategory(
                    id = 1,
                    name = "TEST",
                    contacts =
                        mutableListOf(
                            createContact(
                                id = 1,
                                campusID = 1,
                                name = "TEST",
                                phone = "031-0000-0000",
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                """
                {
                    phonebook(input: { campus: 1 }) {
                        version
                        categories {
                            seq
                            name
                            entries {
                                seq
                                name
                                phone
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.phonebook",
            )
        assertEquals("1.0", result["version"])
        val categories = result["categories"] as List<*>
        val category = categories[0] as Map<*, *>
        assertEquals(1, category["seq"])
        assertEquals("TEST", category["name"])
        val entries = category["entries"] as List<*>
        val entry1 = entries[0] as Map<*, *>
        assertEquals("TEST", entry1["name"])
        assertEquals("031-0000-0000", entry1["phone"])
    }

    @Test
    @DisplayName("전화부를 이름으로 필터링하는지 테스트")
    fun testFetchContactsWithNameFilter() {
        whenever(contactService.getContactVersion()).thenReturn(
            ContactVersion(name = "1.0", createdAt = now),
        )
        whenever(
            contactService.fetchContacts(
                anyOrNull(),
                anyOrNull(),
                eq("TEST"),
                anyOrNull(),
            ),
        ).thenReturn(
            listOf(
                createCategory(
                    id = 1,
                    name = "TEST",
                    contacts =
                        mutableListOf(
                            createContact(
                                id = 1,
                                name = "TEST",
                                phone = "031-0000-0000",
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                """
                {
                    phonebook(input: { name: "TEST" }) {
                        version
                        categories {
                            seq
                            name
                            entries {
                                seq
                                name
                                phone
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.phonebook",
            )
        assertEquals("1.0", result["version"])
        val categories = result["categories"] as List<*>
        val category = categories[0] as Map<*, *>
        assertEquals(1, category["seq"])
        assertEquals("TEST", category["name"])
        val entries = category["entries"] as List<*>
        val entry1 = entries[0] as Map<*, *>
        assertEquals("TEST", entry1["name"])
        assertEquals("031-0000-0000", entry1["phone"])
    }

    @Test
    @DisplayName("전화부를 전화번호로 필터링하는지 테스트")
    fun testFetchContactsWithPhoneFilter() {
        whenever(contactService.getContactVersion()).thenReturn(
            ContactVersion(name = "1.0", createdAt = now),
        )
        whenever(
            contactService.fetchContacts(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq("031"),
            ),
        ).thenReturn(
            listOf(
                createCategory(
                    id = 1,
                    name = "TEST",
                    contacts =
                        mutableListOf(
                            createContact(
                                id = 1,
                                name = "TEST",
                                phone = "031-0000-0000",
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                """
                {
                    phonebook(input: { phone: "031" }) {
                        version
                        categories {
                            seq
                            name
                            entries {
                                seq
                                name
                                phone
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.phonebook",
            )
        assertEquals("1.0", result["version"])
        val categories = result["categories"] as List<*>
        val category = categories[0] as Map<*, *>
        assertEquals(1, category["seq"])
        assertEquals("TEST", category["name"])
        val entries = category["entries"] as List<*>
        val entry1 = entries[0] as Map<*, *>
        assertEquals("TEST", entry1["name"])
        assertEquals("031-0000-0000", entry1["phone"])
    }
}
